package com.example.authbackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "voice_interview_messages")
public class VoiceInterviewMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "role")
    private String role;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "audio_url")
    private String audioUrl;

    @Column(name = "duration")
    private Double duration;

    @Column(name = "speech_rate")
    private Integer speechRate;

    @Column(name = "pause_count")
    private Integer pauseCount;

    @Column(name = "energy_stability")
    private Double energyStability;

    @Column(name = "pitch_variation")
    private Double pitchVariation;

    @Column(name = "filler_word_count")
    private Integer fillerWordCount;

    @Column(name = "emotion_tendency")
    private String emotionTendency;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public VoiceInterviewMessage() {}

    public VoiceInterviewMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public Double getDuration() { return duration; }
    public void setDuration(Double duration) { this.duration = duration; }

    public Integer getSpeechRate() { return speechRate; }
    public void setSpeechRate(Integer speechRate) { this.speechRate = speechRate; }

    public Integer getPauseCount() { return pauseCount; }
    public void setPauseCount(Integer pauseCount) { this.pauseCount = pauseCount; }

    public Double getEnergyStability() { return energyStability; }
    public void setEnergyStability(Double energyStability) { this.energyStability = energyStability; }

    public Double getPitchVariation() { return pitchVariation; }
    public void setPitchVariation(Double pitchVariation) { this.pitchVariation = pitchVariation; }

    public Integer getFillerWordCount() { return fillerWordCount; }
    public void setFillerWordCount(Integer fillerWordCount) { this.fillerWordCount = fillerWordCount; }

    public String getEmotionTendency() { return emotionTendency; }
    public void setEmotionTendency(String emotionTendency) { this.emotionTendency = emotionTendency; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}