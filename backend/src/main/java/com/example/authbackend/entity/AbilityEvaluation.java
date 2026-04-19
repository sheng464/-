package com.example.authbackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ability_evaluations")
public class AbilityEvaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "session_type")
    private String sessionType;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "position")
    private String position;

    @Column(name = "overall_score")
    private Double overallScore;

    @Column(name = "technical_correctness")
    private Double technicalCorrectness;

    @Column(name = "technical_correctness_detail", columnDefinition = "TEXT")
    private String technicalCorrectnessDetail;

    @Column(name = "knowledge_depth")
    private Double knowledgeDepth;

    @Column(name = "knowledge_depth_detail", columnDefinition = "TEXT")
    private String knowledgeDepthDetail;

    @Column(name = "logic_rigorous")
    private Double logicRigorous;

    @Column(name = "logic_rigorous_detail", columnDefinition = "TEXT")
    private String logicRigorousDetail;

    @Column(name = "speech_rate_score")
    private Double speechRateScore;

    @Column(name = "clarity_score")
    private Double clarityScore;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "expression_detail", columnDefinition = "TEXT")
    private String expressionDetail;

    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;

    @Column(name = "weaknesses", columnDefinition = "TEXT")
    private String weaknesses;

    @Column(name = "improvement_suggestions", columnDefinition = "TEXT")
    private String improvementSuggestions;

    @Column(name = "full_analysis", columnDefinition = "TEXT")
    private String fullAnalysis;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public AbilityEvaluation() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public String getSessionType() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public Double getOverallScore() { return overallScore; }
    public void setOverallScore(Double overallScore) { this.overallScore = overallScore; }

    public Double getTechnicalCorrectness() { return technicalCorrectness; }
    public void setTechnicalCorrectness(Double technicalCorrectness) { this.technicalCorrectness = technicalCorrectness; }

    public String getTechnicalCorrectnessDetail() { return technicalCorrectnessDetail; }
    public void setTechnicalCorrectnessDetail(String technicalCorrectnessDetail) { this.technicalCorrectnessDetail = technicalCorrectnessDetail; }

    public Double getKnowledgeDepth() { return knowledgeDepth; }
    public void setKnowledgeDepth(Double knowledgeDepth) { this.knowledgeDepth = knowledgeDepth; }

    public String getKnowledgeDepthDetail() { return knowledgeDepthDetail; }
    public void setKnowledgeDepthDetail(String knowledgeDepthDetail) { this.knowledgeDepthDetail = knowledgeDepthDetail; }

    public Double getLogicRigorous() { return logicRigorous; }
    public void setLogicRigorous(Double logicRigorous) { this.logicRigorous = logicRigorous; }

    public String getLogicRigorousDetail() { return logicRigorousDetail; }
    public void setLogicRigorousDetail(String logicRigorousDetail) { this.logicRigorousDetail = logicRigorousDetail; }

    public Double getSpeechRateScore() { return speechRateScore; }
    public void setSpeechRateScore(Double speechRateScore) { this.speechRateScore = speechRateScore; }

    public Double getClarityScore() { return clarityScore; }
    public void setClarityScore(Double clarityScore) { this.clarityScore = clarityScore; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getExpressionDetail() { return expressionDetail; }
    public void setExpressionDetail(String expressionDetail) { this.expressionDetail = expressionDetail; }

    public String getStrengths() { return strengths; }
    public void setStrengths(String strengths) { this.strengths = strengths; }

    public String getWeaknesses() { return weaknesses; }
    public void setWeaknesses(String weaknesses) { this.weaknesses = weaknesses; }

    public String getImprovementSuggestions() { return improvementSuggestions; }
    public void setImprovementSuggestions(String improvementSuggestions) { this.improvementSuggestions = improvementSuggestions; }

    public String getFullAnalysis() { return fullAnalysis; }
    public void setFullAnalysis(String fullAnalysis) { this.fullAnalysis = fullAnalysis; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}