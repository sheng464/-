from faster_whisper import WhisperModel
from flask import Flask, request, jsonify
import numpy as np
import librosa
import io
import sys

app = Flask(__name__)

def log(msg):
    print(f"[WhisperServer] {msg}", flush=True)
    sys.stdout.flush()

print("Loading Whisper model...")
model = WhisperModel("small", device="cpu", compute_type="int8")
print("Model loaded successfully!")

def load_audio(audio_data):
    log(f"Loading audio, size: {len(audio_data)} bytes")

    log("Trying method 1: soundfile...")
    try:
        import soundfile as sf
        audio_np, sample_rate = sf.read(io.BytesIO(audio_data))
        if len(audio_np.shape) > 1:
            audio_np = audio_np.mean(axis=1)
        audio_np = audio_np.astype(np.float32)
        log(f"soundfile SUCCESS: {len(audio_np)} samples at {sample_rate}Hz")
        return audio_np, sample_rate
    except Exception as e:
        log(f"soundfile FAILED: {e}")

    log("Trying method 2: librosa...")
    try:
        audio_np, sample_rate = librosa.load(io.BytesIO(audio_data), sr=16000, mono=True)
        log(f"librosa SUCCESS: {len(audio_np)} samples at {sample_rate}Hz")
        return audio_np, sample_rate
    except Exception as e:
        log(f"librosa FAILED: {e}")

    log("Trying method 3: PyAV...")
    try:
        import av
        log(f"PyAV: Opening container with {len(audio_data)} bytes")
        container = av.open(io.BytesIO(audio_data))
        log(f"PyAV: Container opened, streams: {len(container.streams)}")

        audio_stream = None
        for stream in container.streams:
            log(f"PyAV: Found stream type: {stream.type}")
            if stream.type == 'audio':
                audio_stream = stream
                break

        if audio_stream is None:
            log("PyAV: No audio stream found")
            container.close()
            raise Exception("No audio stream")

        log(f"PyAV: Audio stream found, codec: {audio_stream.codec_context.name}")

        frames = []
        for packet in container.demux(audio_stream):
            log(f"PyAV: Got packet")
            for frame in packet.decode():
                log(f"PyAV: Decoded frame type: {type(frame)}")
                frames.append(frame)

        log(f"PyAV: Total frames decoded: {len(frames)}")

        if not frames:
            container.close()
            raise Exception("No frames decoded")

        audio_arrays = [frame.to_ndarray() for frame in frames]
        log(f"PyAV: Converted {len(audio_arrays)} arrays, first shape: {audio_arrays[0].shape if audio_arrays else 'empty'}")

        for i, arr in enumerate(audio_arrays):
            if len(arr.shape) > 1:
                audio_arrays[i] = arr.flatten()

        audio_np = np.concatenate(audio_arrays)
        log(f"PyAV: Concatenated shape: {audio_np.shape}")

        if len(audio_np.shape) > 1:
            audio_np = audio_np.mean(axis=1)

        audio_np = audio_np.astype(np.float32)
        log(f"PyAV: Final shape: {audio_np.shape}, dtype: {audio_np.dtype}")

        container.close()

        target_sr = 16000
        if len(audio_np) > 0:
            log(f"PyAV: Resampling from unknown rate to {target_sr}")
            audio_np = librosa.resample(audio_np, orig_sr=audio_stream.rate, target_sr=target_sr)

        log(f"PyAV SUCCESS: {len(audio_np)} samples at {target_sr}Hz")
        return audio_np, target_sr
    except Exception as e:
        log(f"PyAV FAILED: {e}")
        import traceback
        traceback.print_exc(file=sys.stdout)

    raise Exception("Failed to load audio with any method")

def extract_audio_features(audio_data, sample_rate=16000):
    try:
        audio = np.frombuffer(audio_data, dtype=np.float32)

        if len(audio) == 0:
            return {
                "duration": 0,
                "speechRate": 0,
                "pauseCount": 0,
                "pauseTotalSeconds": 0,
                "energyStability": 0,
                "pitchVariation": 0,
                "fillerWordCount": 0,
                "emotionTendency": "unknown"
            }

        duration = len(audio) / sample_rate

        rms = librosa.feature.rms(y=audio, frame_length=512, hop_length=256)[0]
        energy_stability = float(np.mean(rms) / (np.std(rms) + 1e-8))
        energy_stability = max(0, min(1, energy_stability / 50))

        zcr = librosa.feature.zero_crossing_rate(y=audio, frame_length=512, hop_length=256)[0]
        pitch_variation = float(np.std(zcr) * 50)
        pitch_variation = max(0, min(1, pitch_variation))

        try:
            onset_env = librosa.onset.onset_strength(y=audio, sr=sample_rate)
            speech_rate = librosa.beat.tempo(onset_envelope=onset_env, sr=sample_rate)[0]
            speech_rate_wpm = int(speech_rate * 60 / 2)
        except Exception as e:
            log(f"Speech rate estimation failed: {e}")
            speech_rate_wpm = 0

        frame_length = int(0.03 * sample_rate)
        hop_length = int(0.01 * sample_rate)
        energy = []
        for i in range(0, len(audio) - frame_length, hop_length):
            frame = audio[i:i + frame_length]
            energy.append(np.sqrt(np.mean(frame ** 2)))

        threshold = np.mean(energy) * 0.3
        pause_frames = 0
        consecutive = 0
        for e in energy:
            if e < threshold:
                consecutive += 1
            else:
                if consecutive > 15:
                    pause_frames += 1
                consecutive = 0

        pause_total_seconds = pause_frames * 0.01 * 3

        filler_ratio = pause_frames / (duration + 0.001)
        filler_word_count = min(int(filler_ratio * 0.5), 20)

        if energy_stability > 0.6 and pitch_variation > 0.3 and speech_rate_wpm > 100:
            emotion = "confident"
        elif energy_stability < 0.4 or speech_rate_wpm < 80:
            emotion = "nervous"
        elif pitch_variation > 0.6:
            emotion = "excited"
        else:
            emotion = "calm"

        return {
            "duration": round(duration, 1),
            "speechRate": speech_rate_wpm,
            "pauseCount": pause_frames,
            "pauseTotalSeconds": round(pause_total_seconds, 1),
            "energyStability": round(energy_stability, 2),
            "pitchVariation": round(pitch_variation, 2),
            "fillerWordCount": filler_word_count,
            "emotionTendency": emotion
        }
    except Exception as e:
        log(f"Audio feature extraction error: {e}")
        return {
            "duration": 0,
            "speechRate": 0,
            "pauseCount": 0,
            "pauseTotalSeconds": 0,
            "energyStability": 0,
            "pitchVariation": 0,
            "fillerWordCount": 0,
            "emotionTendency": "unknown"
        }

@app.route('/v1/audio/transcriptions', methods=['POST'])
def transcribe():
    log("=== INCOMING REQUEST ===")
    try:
        audio_data = request.data
        log(f"Processing audio of size: {len(audio_data)} bytes")

        audio_np, sample_rate = load_audio(audio_data)
        log(f"Loaded audio: {len(audio_np)} samples, rate: {sample_rate}")

        log("Transcribing...")
        segments, info = model.transcribe(audio_np, language="zh")
        text = ' '.join([s.text for s in segments])
        log(f"Transcription: {text}")

        log("Extracting audio features...")
        audio_features = extract_audio_features(audio_np, sample_rate)
        log(f"Audio features: {audio_features}")

        return jsonify({
            "text": text,
            "language": info.language,
            "language_probability": info.language_probability,
            "audio_features": audio_features
        })

    except Exception as e:
        log(f"Error: {str(e)}")
        import traceback
        traceback.print_exc(file=sys.stdout)
        return jsonify({"error": str(e)}), 500

@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "ok"})

if __name__ == '__main__':
    print("Starting Whisper server on http://localhost:5000")
    print("API endpoint: http://localhost:5000/v1/audio/transcriptions")
    app.run(host='0.0.0.0', port=5000, threaded=True, debug=False)