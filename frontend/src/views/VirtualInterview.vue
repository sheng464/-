<template>
  <div class="chat-container">
    <div class="chat-header">
      <button class="back-btn" @click="goBack">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="19" y1="12" x2="5" y2="12"></line>
          <polyline points="12 19 5 12 12 5"></polyline>
        </svg>
        返回
      </button>
      <div class="header-info">
        <h1>{{ position }} - AI面试</h1>
        <div class="interview-progress">
          <span class="progress-item">问题 {{ questionCount }}/{{ maxQuestionsFromBackend || maxQuestions }}</span>
        </div>
      </div>
      <div class="header-status" :class="{ connected: isConnected && !interviewEnded }">
        <span class="status-dot"></span>
        {{ interviewEnded ? '已结束' : (isConnected ? '进行中' : '连接失败') }}
      </div>
    </div>

    <div class="chat-messages" ref="messagesContainer">
      <div v-if="messages.length === 0" class="welcome-message">
        <div class="welcome-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
            <circle cx="12" cy="7" r="4"></circle>
          </svg>
        </div>
        <h2>欢迎参加AI模拟面试</h2>
        <p>面试即将开始，AI面试官将向您提问</p>
      </div>

      <div v-for="(msg, index) in messages" :key="index" class="message" :class="msg.role">
        <div class="message-avatar">
          <svg v-if="msg.role === 'assistant'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <path d="M12 16v-4"></path>
            <path d="M12 8h.01"></path>
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
            <circle cx="12" cy="7" r="4"></circle>
          </svg>
        </div>
        <div class="message-content">
          <div v-if="msg.messageType === 'voice'" class="voice-message">
            <button class="voice-play-btn" @click="playVoiceMessage(msg)">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="5 3 19 12 5 21 5 3"></polygon>
              </svg>
            </button>
            <div class="voice-waveform">
              <span v-for="i in 5" :key="i" class="wave-bar"></span>
            </div>
            <span class="voice-duration">{{ msg.audioFeatures ? msg.audioFeatures.duration + 's' : '' }}</span>
          </div>
          <div v-else class="message-text">{{ msg.content }}</div>
          <div class="message-time">{{ msg.time }}</div>
        </div>
      </div>

      <div v-if="isTyping" class="message assistant">
        <div class="message-avatar">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <path d="M12 16v-4"></path>
            <path d="M12 8h.01"></path>
          </svg>
        </div>
        <div class="message-content">
          <div class="message-text typing">
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
          </div>
        </div>
      </div>
    </div>

    <div class="chat-input-area" v-if="isVoiceMode">
      <button
        class="voice-btn large"
        :class="{ recording: isRecording, uploading: isUploading, ended: interviewEnded }"
        :disabled="!isConnected || isUploading || interviewEnded"
        @click="toggleRecording"
      >
        <svg v-if="!isRecording && !isUploading && !interviewEnded" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"></path>
          <path d="M19 10v2a7 7 0 0 1-14 0v-2"></path>
          <line x1="12" y1="19" x2="12" y2="23"></line>
          <line x1="8" y1="23" x2="16" y2="23"></line>
        </svg>
        <svg v-else-if="isUploading && !interviewEnded" class="uploading-spinner" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"></circle>
          <path d="M12 6v6l4 2"></path>
        </svg>
        <svg v-else-if="interviewEnded" class="ended-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="20 6 9 17 4 12"></polyline>
        </svg>
        <div v-else class="recording-indicator">
          <span class="recording-dot"></span>
        </div>
      </button>
      <span v-if="isRecording" class="recording-time">{{ formatTime(recordingDuration) }}</span>
      <p class="voice-hint" :class="{ ended: interviewEnded }">
        <template v-if="interviewEnded">面试已结束</template>
        <template v-else-if="isRecording">正在录音，点击停止</template>
        <template v-else>点击开始录音</template>
      </p>
    </div>

    <div class="chat-input-area" v-else>
      <input
        type="text"
        v-model="inputMessage"
        :disabled="!isConnected || isRecording || isUploading"
        @keyup.enter="sendTextMessage"
        placeholder="输入您的回答..."
      />
      <button class="send-btn" :disabled="!inputMessage.trim() || !isConnected || isRecording || isUploading" @click="sendTextMessage">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="22" y1="2" x2="11" y2="13"></line>
          <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
        </svg>
      </button>
    </div>

    <div v-if="interviewEnded" class="evaluation-banner">
      <p>🎉 面试已完成</p>
      <button class="view-evaluation-btn" @click="goToInterviewStart">
        返回面试首页
      </button>
    </div>

    <audio ref="audioPlayer" @ended="onAudioEnded" style="display: none"></audio>
  </div>
</template>

<script>
export default {
  name: 'VirtualInterview',
  props: {
    sessionId: String,
    position: String,
    sessionType: String,
    maxQuestions: {
      type: Number,
      default: 10
    },
    maxFollowups: {
      type: Number,
      default: 3
    },
    initialMessage: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      inputMessage: '',
      messages: [],
      isTyping: false,
      isConnected: false,
      questionCount: 0,
      followupCount: 0,
      interviewEnded: false,
      maxQuestionsFromBackend: null,
      isRecording: false,
      isUploading: false,
      mediaRecorder: null,
      audioChunks: [],
      recordingDuration: 0,
      recordingTimer: null,
      audioPlayer: null
    }
  },
  watch: {
    interviewEnded(newVal) {
      if (newVal) {
        this.cleanup()
      }
    }
  },
  beforeDestroy() {
    this.cleanup()
  },
  computed: {
    isVoiceMode() {
      return this.sessionType === 'VOICE'
    }
  },
  mounted() {
    this.isConnected = true
    this.audioPlayer = this.$refs.audioPlayer

    if (this.initialMessage) {
      this.messages.push({
        role: 'assistant',
        content: this.initialMessage,
        time: new Date().toLocaleTimeString()
      })
      this.questionCount = 1
      this.scrollToBottom()
    } else if (this.sessionType === 'VOICE') {
      this.startVoiceInterview()
    } else {
      this.startInterviewViaApi()
    }
  },
  methods: {
    goBack() {
      if (this.isRecording) {
        this.stopRecording()
      }
      this.$router.push('/interview/start')
    },
    formatTime(seconds) {
      const mins = Math.floor(seconds / 60)
      const secs = seconds % 60
      return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
    },
    async toggleRecording() {
      if (this.isRecording) {
        this.stopRecording()
      } else {
        await this.startRecording()
      }
    },
    async startRecording() {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
        this.mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm' })
        this.audioChunks = []

        this.mediaRecorder.ondataavailable = (e) => {
          if (e.data.size > 0) {
            this.audioChunks.push(e.data)
          }
        }

        this.mediaRecorder.onstop = async () => {
          stream.getTracks().forEach(track => track.stop())
          await this.uploadAudio()
        }

        this.mediaRecorder.start()
        this.isRecording = true
        this.recordingDuration = 0
        this.recordingTimer = setInterval(() => {
          this.recordingDuration++
        }, 1000)
      } catch (err) {
        alert('无法访问麦克风，请检查权限设置')
        console.error('Recording error:', err)
      }
    },
    stopRecording() {
      if (this.mediaRecorder && this.isRecording) {
        this.mediaRecorder.stop()
        this.isRecording = false
        if (this.recordingTimer) {
          clearInterval(this.recordingTimer)
          this.recordingTimer = null
        }
      }
    },
    async uploadAudio() {
      if (this.audioChunks.length === 0) return

      this.isUploading = true
      const audioBlob = new Blob(this.audioChunks, { type: 'audio/webm' })

      const userMessage = {
        role: 'user',
        content: '[语音消息]',
        time: new Date().toLocaleTimeString(),
        messageType: 'voice',
        audioBlob: audioBlob
      }
      this.messages.push(userMessage)
      this.scrollToBottom()

      this.isTyping = true

      try {
        const token = localStorage.getItem('token')
        const formData = new FormData()
        formData.append('audio', audioBlob, 'recording.webm')
        formData.append('sessionId', this.sessionId)

        const response = await fetch('http://localhost:8080/api/interview/voice', {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`
          },
          body: formData
        })

        const data = await response.json()
        this.isTyping = false
        this.isUploading = false

        if (data.success) {
          const replyData = data.data
          userMessage.content = replyData.transcript || '[语音消息]'
          userMessage.audioFeatures = replyData.audioFeatures

          this.messages.push({
            role: 'assistant',
            content: replyData.reply,
            time: new Date().toLocaleTimeString(),
            messageType: 'text'
          })
          this.questionCount = replyData.questionCount
          this.followupCount = replyData.followupCount
          this.interviewEnded = replyData.interviewEnded
          if (replyData.maxQuestions) {
            this.maxQuestionsFromBackend = replyData.maxQuestions
          }

          if (replyData.audioUrl) {
            this.messages.push({
              role: 'assistant',
              content: '[AI语音回答]',
              time: new Date().toLocaleTimeString(),
              messageType: 'voice',
              audioUrl: replyData.audioUrl
            })
            if (this.audioPlayer) {
              this.audioPlayer.src = replyData.audioUrl
              this.audioPlayer.play()
            }
          }
        } else {
          this.messages.push({
            role: 'assistant',
            content: '抱歉，发生了错误：' + data.message,
            time: new Date().toLocaleTimeString(),
            messageType: 'text'
          })
        }
      } catch (err) {
        this.isTyping = false
        this.isUploading = false
        this.messages.push({
          role: 'assistant',
          content: '网络错误，请稍后重试',
          time: new Date().toLocaleTimeString(),
          messageType: 'text'
        })
      }

      this.scrollToBottom()
    },
    onAudioEnded() {
      console.log('Audio playback ended')
    },
    playVoiceMessage(msg) {
      if (msg.audioBlob) {
        const url = URL.createObjectURL(msg.audioBlob)
        const audio = new Audio(url)
        audio.play()
      } else if (msg.audioUrl) {
        if (this.audioPlayer) {
          this.audioPlayer.src = msg.audioUrl
          this.audioPlayer.play()
        }
      }
    },
    async startInterviewViaApi() {
      this.isTyping = true

      try {
        const token = localStorage.getItem('token')
        const response = await fetch(
          `http://localhost:8080/api/interview/chat/stream/${this.sessionId}?message=`,
          {
            headers: {
              Authorization: `Bearer ${token}`
            }
          }
        )

        if (!response.ok) {
          throw new Error('Network response was not ok')
        }

        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''

        const assistantMessage = {
          role: 'assistant',
          content: '',
          time: new Date().toLocaleTimeString()
        }
        this.messages.push(assistantMessage)

        while (true) {
          const { done, value } = await reader.read()
          if (done) break

          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop() || ''

          for (const line of lines) {
            if (line.startsWith('data:')) {
              const data = line.slice(5).trim()
              if (data) {
                if (data === '[DONE]') continue
                try {
                  const event = JSON.parse(data)
                  if (event.chunk) {
                    assistantMessage.content += event.chunk
                    this.scrollToBottom()
                  }
                  if (event.interviewEnded) {
                    this.interviewEnded = event.interviewEnded
                  }
                  if (event.questionCount !== undefined) {
                    this.questionCount = event.questionCount
                    this.followupCount = event.followupCount || this.followupCount
                    if (event.maxQuestions) {
                      this.maxQuestionsFromBackend = event.maxQuestions
                    }
                  }
                  if (event.message && this.interviewEnded) {
                    assistantMessage.content = event.message
                  }
                } catch (e) {
                  assistantMessage.content += data
                  this.scrollToBottom()
                }
              }
            }
          }
        }

        this.isTyping = false
        this.scrollToBottom()
      } catch (err) {
        this.isTyping = false
        console.error('Stream error:', err)
      }
    },
    async startInterview() {
      this.isTyping = true
      try {
        const token = localStorage.getItem('token')
        const response = await fetch('http://localhost:8080/api/interview/chat', {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            message: '',
            sessionId: this.sessionId
          })
        })

        const data = await response.json()
        this.isTyping = false

        if (data.success) {
          const replyData = data.data
          this.messages.push({
            role: 'assistant',
            content: replyData.reply,
            time: new Date().toLocaleTimeString()
          })
          this.questionCount = replyData.questionCount
          this.followupCount = replyData.followupCount
          this.interviewEnded = replyData.interviewEnded
          if (replyData.maxQuestions) {
            this.maxQuestionsFromBackend = replyData.maxQuestions
          }
          this.scrollToBottom()
        }
      } catch (err) {
        this.isTyping = false
      }
    },
    createVoiceRequest(token) {
      return null
    },
    async startVoiceInterview() {
      this.isTyping = true
      try {
        const token = localStorage.getItem('token')
        const formData = new FormData()
        formData.append('sessionId', this.sessionId)

        const response = await fetch('http://localhost:8080/api/interview/voice/start', {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`
          },
          body: formData
        })

        const data = await response.json()
        this.isTyping = false

        if (data.success) {
          const replyData = data.data
          this.messages.push({
            role: 'assistant',
            content: replyData.reply,
            time: new Date().toLocaleTimeString(),
            messageType: 'text'
          })
          this.questionCount = replyData.questionCount
          this.followupCount = replyData.followupCount
          this.interviewEnded = replyData.interviewEnded
          if (replyData.maxQuestions) {
            this.maxQuestionsFromBackend = replyData.maxQuestions
          }
          this.scrollToBottom()
        }
      } catch (err) {
        this.isTyping = false
      }
    },
    async sendTextMessage() {
      if (this.sessionType === 'VOICE') return
      if (!this.inputMessage.trim() || this.isTyping || this.interviewEnded || this.isRecording || this.isUploading) return

      const message = this.inputMessage.trim()
      this.inputMessage = ''

      const userMessage = {
        role: 'user',
        content: message,
        time: new Date().toLocaleTimeString(),
        messageType: 'text'
      }
      this.messages.push(userMessage)
      this.scrollToBottom()

      this.isTyping = true

      try {
        const token = localStorage.getItem('token')
        const response = await fetch(
          `http://localhost:8080/api/interview/chat/stream/${this.sessionId}?message=${encodeURIComponent(message)}`,
          {
            headers: {
              Authorization: `Bearer ${token}`
            }
          }
        )

        if (!response.ok) {
          throw new Error('Network response was not ok')
        }

        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''

        const assistantMessage = {
          role: 'assistant',
          content: '',
          time: new Date().toLocaleTimeString(),
          messageType: 'text'
        }
        this.messages.push(assistantMessage)

        while (true) {
          const { done, value } = await reader.read()
          if (done) break

          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop() || ''

          for (const line of lines) {
            if (line.startsWith('data:')) {
              const data = line.slice(5).trim()
              if (data) {
                if (data === '[DONE]') continue
                try {
                  const event = JSON.parse(data)
                  if (event.chunk) {
                    assistantMessage.content += event.chunk
                    this.scrollToBottom()
                  }
                  if (event.interviewEnded) {
                    this.interviewEnded = event.interviewEnded
                  }
                  if (event.questionCount !== undefined) {
                    this.questionCount = event.questionCount
                    this.followupCount = event.followupCount || this.followupCount
                    if (event.maxQuestions) {
                      this.maxQuestionsFromBackend = event.maxQuestions
                    }
                  }
                  if (event.message && this.interviewEnded) {
                    assistantMessage.content = event.message
                  }
                } catch (e) {
                  assistantMessage.content += data
                  this.scrollToBottom()
                }
              }
            }
          }
        }

        this.isTyping = false
        this.scrollToBottom()
      } catch (err) {
        this.isTyping = false
        this.messages.push({
          role: 'assistant',
          content: '网络错误，请稍后重试',
          time: new Date().toLocaleTimeString(),
          messageType: 'text'
        })
        this.scrollToBottom()
      }
    },
    scrollToBottom() {
      setTimeout(() => {
        if (this.$refs.messagesContainer) {
          this.$refs.messagesContainer.scrollTop = this.$refs.messagesContainer.scrollHeight
        }
      }, 100)
    },
    cleanup() {
      if (this.recordingTimer) {
        clearInterval(this.recordingTimer)
        this.recordingTimer = null
      }
      if (this.mediaRecorder) {
        this.mediaRecorder = null
      }
    },
    goToInterviewStart() {
      this.cleanup()
      this.$router.push('/interview/start')
    },
    async showEvaluationReport() {
      const token = localStorage.getItem('token')
      if (!token) return

      try {
        const response = await fetch(`http://localhost:8080/api/interview/evaluation/${this.sessionId}`, {
          headers: { Authorization: `Bearer ${token}` }
        })
        const data = await response.json()
        if (data.success) {
          sessionStorage.setItem('evaluationReport', JSON.stringify(data.data))
          this.$router.push({ name: 'EvaluationReport', query: { sessionId: this.sessionId } })
        }
      } catch (err) {
        console.error('获取评估报告失败:', err)
      }
    }
  }
}
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
  max-width: 900px;
  margin: 0 auto;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px 20px;
  background: white;
  border-radius: 15px;
  margin-bottom: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 8px 15px;
  background: #f5f5f5;
  border: none;
  border-radius: 8px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s;
}

.back-btn:hover {
  background: #eee;
}

.back-btn svg {
  width: 18px;
  height: 18px;
}

.header-info h1 {
  font-size: 18px;
  color: #333;
  margin: 0;
}

.interview-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  font-size: 12px;
  color: #666;
}

.progress-divider {
  color: #ddd;
}

.header-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #999;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ccc;
}

.header-status.connected .status-dot {
  background: #4caf50;
}

.header-status.connected {
  color: #4caf50;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: white;
  border-radius: 15px;
  margin-bottom: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.welcome-message {
  text-align: center;
  padding: 60px 20px;
}

.welcome-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.welcome-icon svg {
  width: 40px;
  height: 40px;
  color: white;
}

.welcome-message h2 {
  font-size: 24px;
  color: #333;
  margin-bottom: 10px;
}

.welcome-message p {
  color: #666;
  font-size: 14px;
}

.message {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 40px;
  height: 40px;
  min-width: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.message.assistant .message-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.message.user .message-avatar {
  background: #4caf50;
}

.message-avatar svg {
  width: 20px;
  height: 20px;
  color: white;
}

.message-content {
  max-width: 70%;
}

.message.user .message-content {
  text-align: right;
}

.message-text {
  padding: 12px 18px;
  border-radius: 15px;
  font-size: 15px;
  line-height: 1.5;
  word-break: break-word;
}

.message.assistant .message-text {
  background: #f5f5f5;
  color: #333;
  border-bottom-left-radius: 5px;
}

.message.user .message-text {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-bottom-right-radius: 5px;
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 5px;
}

.message.user .message-time {
  text-align: right;
}

.typing .typing-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  background: #999;
  border-radius: 50%;
  margin: 0 3px;
  animation: typing 1.4s infinite ease-in-out both;
}

.typing .typing-dot:nth-child(1) {
  animation-delay: -0.32s;
}

.typing .typing-dot:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.chat-input-area {
  display: flex;
  gap: 10px;
  padding: 15px 20px;
  background: white;
  border-radius: 15px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.chat-input-area input {
  flex: 1;
  padding: 14px 18px;
  border: 2px solid #eee;
  border-radius: 25px;
  font-size: 15px;
  outline: none;
  transition: border-color 0.3s;
}

.chat-input-area input:focus {
  border-color: #667eea;
}

.chat-input-area input:disabled {
  background: #f5f5f5;
}

.send-btn {
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.05);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.send-btn svg {
  width: 22px;
  height: 22px;
  color: white;
}

.voice-btn {
  width: 50px;
  height: 50px;
  background: #f5f5f5;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.voice-btn.large {
  width: 80px;
  height: 80px;
}

.voice-btn.ended {
  background: #4caf50 !important;
}

.voice-btn.ended svg {
  color: white;
}

.voice-hint.ended {
  color: #4caf50;
  font-weight: 600;
}

.ended-icon {
  width: 30px;
  height: 30px;
  color: white;
}

.voice-hint {
  font-size: 14px;
  color: #666;
  text-align: center;
  margin-top: 10px;
}

.voice-btn:hover:not(:disabled) {
  background: #eee;
  transform: scale(1.05);
}

.voice-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.voice-btn.recording {
  background: #f44336;
  animation: pulse 1s infinite;
}

.voice-btn.uploading {
  background: #ff9800;
}

.voice-btn svg {
  width: 22px;
  height: 22px;
  color: #666;
}

.voice-btn.recording svg {
  color: white;
}

.voice-btn.uploading svg {
  color: white;
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.1); }
  100% { transform: scale(1); }
}

.uploading-spinner {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.recording-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
}

.recording-dot {
  width: 12px;
  height: 12px;
  background: white;
  border-radius: 50%;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.recording-time {
  font-size: 14px;
  color: #f44336;
  font-weight: 600;
  min-width: 50px;
  text-align: center;
}

.voice-message {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 15px;
  background: rgba(102, 126, 234, 0.1);
  border-radius: 20px;
  min-width: 120px;
}

.voice-play-btn {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.voice-play-btn svg {
  width: 14px;
  height: 14px;
  color: white;
  margin-left: 2px;
}

.voice-waveform {
  display: flex;
  align-items: center;
  gap: 3px;
  height: 20px;
}

.wave-bar {
  width: 3px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 2px;
  animation: wave 1s ease-in-out infinite;
}

.wave-bar:nth-child(1) { height: 8px; animation-delay: 0s; }
.wave-bar:nth-child(2) { height: 14px; animation-delay: 0.1s; }
.wave-bar:nth-child(3) { height: 20px; animation-delay: 0.2s; }
.wave-bar:nth-child(4) { height: 14px; animation-delay: 0.3s; }
.wave-bar:nth-child(5) { height: 8px; animation-delay: 0.4s; }

@keyframes wave {
  0%, 100% { transform: scaleY(0.5); }
  50% { transform: scaleY(1); }
}

.voice-duration {
  font-size: 12px;
  color: #666;
  min-width: 30px;
}

.evaluation-banner {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.2);
  z-index: 100;
}

.evaluation-banner p {
  font-size: 18px;
  font-weight: 600;
  color: white;
  margin: 0;
}

.view-evaluation-btn {
  padding: 12px 30px;
  background: white;
  color: #667eea;
  border: none;
  border-radius: 25px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.view-evaluation-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.2);
}
</style>