package com.example.authbackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.authbackend.dto.MessageContext;

@Entity
@Table(name = "interview_sessions")
public class InterviewSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "position")
    private String position;

    @Column(name = "resume_id")
    private Long resumeId;

    @Column(name = "resume_text", columnDefinition = "TEXT")
    private String resumeText;

    @Column(name = "status")
    private String status;

    @Column(name = "session_type")
    private String sessionType;

    @Column(name = "interview_type")
    private String interviewType;

    @Column(name = "question_count")
    private Integer questionCount = 0;

    @Column(name = "max_questions")
    private Integer maxQuestions;

    @Column(name = "followup_count")
    private Integer followupCount = 0;

    @Column(name = "max_followups")
    private Integer maxFollowups;

    @Column(name = "last_was_followup")
    private Boolean lastWasFollowup = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private List<MessageContext> tempMessages = new ArrayList<>();

    @Transient
    private long lastActiveTime;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        lastActiveTime = System.currentTimeMillis();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public Long getResumeId() { return resumeId; }
    public void setResumeId(Long resumeId) { this.resumeId = resumeId; }

    public String getResumeText() { return resumeText; }
    public void setResumeText(String resumeText) { this.resumeText = resumeText; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSessionType() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }

    public String getInterviewType() { return interviewType; }
    public void setInterviewType(String interviewType) { this.interviewType = interviewType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getQuestionCount() { return questionCount; }
    public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }

    public Integer getMaxQuestions() { return maxQuestions; }
    public void setMaxQuestions(Integer maxQuestions) { this.maxQuestions = maxQuestions; }

    public Integer getFollowupCount() { return followupCount; }
    public void setFollowupCount(Integer followupCount) { this.followupCount = followupCount; }

    public Integer getMaxFollowups() { return maxFollowups; }
    public void setMaxFollowups(Integer maxFollowups) { this.maxFollowups = maxFollowups; }

    public Boolean getLastWasFollowup() { return lastWasFollowup; }
    public void setLastWasFollowup(Boolean lastWasFollowup) { this.lastWasFollowup = lastWasFollowup; }

    public List<MessageContext> getTempMessages() { return tempMessages; }
    public void setTempMessages(List<MessageContext> tempMessages) { this.tempMessages = tempMessages; }

    public long getLastActiveTime() { return lastActiveTime; }
    public void setLastActiveTime(long lastActiveTime) { this.lastActiveTime = lastActiveTime; }
}