package com.example.authbackend.service;

import com.example.authbackend.entity.*;
import com.example.authbackend.dto.MessageContext;
import com.example.authbackend.repository.*;
import com.example.authbackend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InterviewService {

    public static final String SESSION_TYPE_TEXT = "TEXT";
    public static final String SESSION_TYPE_VOICE = "VOICE";

    @Autowired
    private AIService aiService;

    @Autowired
    private AudioFeatureService audioFeatureService;

    @Autowired
    private MinioService minioService;

    @Autowired
    private InterviewSessionRepository sessionRepository;

    @Autowired
    private TextInterviewMessageRepository textMessageRepository;

    @Autowired
    private VoiceInterviewMessageRepository voiceMessageRepository;

    @Autowired
    private AbilityEvaluationRepository abilityEvaluationRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RagService ragService;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private ExcellentAnswerService excellentAnswerService;

    @Autowired
    private AsyncEvaluationService asyncEvaluationService;

    @Value("${interview.tech.maxQuestions:15}")
    private int techMaxQuestions;

    @Value("${interview.tech.maxFollowups:0}")
    private int techMaxFollowups;

    @Value("${interview.project.maxQuestions:10}")
    private int projectMaxQuestions;

    @Value("${interview.project.maxFollowups:3}")
    private int projectMaxFollowups;

    private final Map<String, InterviewSession> activeSessions = new ConcurrentHashMap<>();

    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000;

    public Map<String, Object> startInterview(MultipartFile file, Long resumeId, String position, String token, String sessionType, String interviewType) throws Exception {
        Long userId = getUserIdFromToken(token);
        String resumeText = extractResumeText(file, resumeId, userId);
        validatePositionMatch(resumeText, position);

        InterviewSession session = createSession(userId, position, resumeId, resumeText, sessionType, interviewType);

        String sessionId = UUID.randomUUID().toString();
        activeSessions.put(sessionId, session);

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", sessionId);
        data.put("message", "请准备好开始面试");
        data.put("maxQuestions", session.getMaxQuestions() != null ? session.getMaxQuestions() : 10);
        data.put("maxFollowups", session.getMaxFollowups() != null ? session.getMaxFollowups() : 3);
        result.put("success", true);
        result.put("data", data);
        return result;
    }

    public Map<String, Object> chat(String message, String sessionId, String token) throws Exception {
        Long userId = getUserIdFromToken(token);
        InterviewSession session = validateSession(sessionId, userId);

        boolean isEmptyMessage = (message == null || message.trim().isEmpty());

        if (!isEmptyMessage && !SESSION_TYPE_TEXT.equals(session.getSessionType())) {
            throw new RuntimeException("会话类型不匹配，请使用语音面试接口");
        }

        if (isEmptyMessage) {
            return handleEmptyMessage(session, sessionId);
        }

        session.getTempMessages().add(new MessageContext("user", message));

        if (Boolean.TRUE.equals(session.getLastWasFollowup())) {
            session.setFollowupCount(session.getFollowupCount() + 1);
        } else {
            session.setQuestionCount(session.getQuestionCount() + 1);
        }

        boolean shouldFollowup = session.getFollowupCount() < session.getMaxFollowups() 
                                && aiService.shouldAskFollowup(session.getTempMessages());

        if (shouldFollowup) {
            String aiResponse = aiService.generateFollowupResponse(
                session.getResumeText(),
                session.getPosition(),
                session.getTempMessages(),
                session.getFollowupCount(),
                session.getMaxFollowups(),
                session.getUserId(),
                session.getResumeId()
            );
            session.setLastWasFollowup(true);
            session.getTempMessages().add(new MessageContext("assistant", aiResponse));
            return buildChatResponse(sessionId, session, aiResponse, true, false);
        }

        if (session.getQuestionCount() >= session.getMaxQuestions()) {
            return generateEndResponse(session, sessionId);
        }

        String aiResponse = aiService.generateNewQuestionResponse(
            session.getResumeText(),
            session.getPosition(),
            session.getTempMessages(),
            session.getUserId(),
            session.getInterviewType()
        );
        session.setLastWasFollowup(false);
        session.setFollowupCount(0);
        session.getTempMessages().add(new MessageContext("assistant", aiResponse));
        return buildChatResponse(sessionId, session, aiResponse, false, false);
    }

    public void chatStream(String message, String sessionId, String token, SseEmitter emitter) {
        try {
            Long userId = getUserIdFromToken(token);
            InterviewSession session = validateSession(sessionId, userId);

            final boolean isEmptyMessage = (message == null || message.trim().isEmpty());

            if (!isEmptyMessage && !SESSION_TYPE_TEXT.equals(session.getSessionType())) {
                emitter.send(SseEmitter.event().name("error").data("会话类型不匹配"));
                emitter.complete();
                return;
            }

            if (isEmptyMessage) {
                System.out.println("[DEBUG] chatStream empty message - interviewType: " + session.getInterviewType());
                String prompt = aiService.buildNewQuestionPrompt(
                    session.getResumeText(),
                    session.getPosition(),
                    session.getTempMessages(),
                    session.getUserId(),
                    session.getInterviewType()
                );

                session.setLastWasFollowup(false);
                session.setFollowupCount(0);

                final StringBuilder fullResponse = new StringBuilder();

                aiService.requestAIStream(prompt, new AIService.StreamCallback() {
                    @Override
                    public void onChunk(String chunk) {
                        fullResponse.append(chunk);
                        try {
                            emitter.send(SseEmitter.event().data(chunk));
                        } catch (Exception e) {
                            System.err.println("[Stream] Send error: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onComplete() {
                        String response = fullResponse.toString();
                        session.getTempMessages().add(new MessageContext("assistant", response));

                        try {
                            Map<String, Object> endData = new HashMap<>();
                            endData.put("questionCount", session.getQuestionCount());
                            endData.put("maxQuestions", session.getMaxQuestions());
                            endData.put("followupCount", session.getFollowupCount());
                            endData.put("maxFollowups", session.getMaxFollowups());
                            endData.put("interviewEnded", false);
                            emitter.send(SseEmitter.event().name("end").data(endData));
                        } catch (Exception e) {}
                        emitter.complete();
                    }

                    @Override
                    public void onError(Exception e) {
                        System.err.println("[Stream] Stream error: " + e.getMessage());
                        try {
                            emitter.send(SseEmitter.event().name("error").data("流式传输错误: " + e.getMessage()));
                        } catch (Exception ex) {}
                        emitter.complete();
                    }
                });
                return;
            }

            session.getTempMessages().add(new MessageContext("user", message));

        if (Boolean.TRUE.equals(session.getLastWasFollowup())) {
            session.setFollowupCount(session.getFollowupCount() + 1);
        } else {
            session.setQuestionCount(session.getQuestionCount() + 1);
        }

        final boolean shouldFollowup = session.getFollowupCount() < session.getMaxFollowups();
        final boolean isTechInterview = "TECH".equals(session.getInterviewType());

            String prompt;
            boolean followupFlag;

            if (shouldFollowup && !isTechInterview) {
                try {
                    boolean aiShouldFollowup = aiService.shouldAskFollowup(session.getTempMessages());
                    if (aiShouldFollowup) {
                        prompt = aiService.buildFollowupPrompt(
                            session.getResumeText(),
                            session.getPosition(),
                            session.getTempMessages(),
                            session.getFollowupCount(),
                            session.getMaxFollowups(),
                            session.getUserId(),
                            session.getResumeId()
                        );
                        followupFlag = true;
                    } else {
                        prompt = aiService.buildNewQuestionPrompt(
                            session.getResumeText(),
                            session.getPosition(),
                            session.getTempMessages(),
                            session.getUserId(),
                            session.getInterviewType()
                        );
                        followupFlag = false;
                    }
                } catch (Exception e) {
                    prompt = aiService.buildNewQuestionPrompt(
                        session.getResumeText(),
                        session.getPosition(),
                        session.getTempMessages(),
                        session.getUserId(),
                        session.getInterviewType()
                    );
                    followupFlag = false;
                }
            } else {
                prompt = aiService.buildNewQuestionPrompt(
                    session.getResumeText(),
                    session.getPosition(),
                    session.getTempMessages(),
                    session.getUserId(),
                    session.getInterviewType()
                );
                followupFlag = false;
            }

            if (!followupFlag && session.getQuestionCount() >= session.getMaxQuestions()) {
                session.setStatus("COMPLETED");
                sessionRepository.save(session);
                saveTextMessages(session);
                asyncEvaluationService.performEvaluationAsync(
                    session.getId(),
                    session.getSessionType(),
                    session.getResumeText(),
                    session.getPosition(),
                    session.getUserId()
                );
                activeSessions.remove(sessionId);
                emitter.send(SseEmitter.event().name("end").data(Map.of(
                    "message", "面试已结束，面试报告正在后端生成中",
                    "interviewEnded", true
                )));
                emitter.complete();
                return;
            }

            final boolean isFollowupResponse = followupFlag;

            final StringBuilder fullResponse = new StringBuilder();

            aiService.requestAIStream(prompt, new AIService.StreamCallback() {
                @Override
                public void onChunk(String chunk) {
                    fullResponse.append(chunk);
                    try {
                        emitter.send(SseEmitter.event().data(chunk));
                    } catch (Exception e) {
                        System.err.println("[Stream] Send error: " + e.getMessage());
                    }
                }

                @Override
                public void onComplete() {
                    String response = fullResponse.toString();
                    final boolean wasFollowup = isFollowupResponse;

                    session.setLastWasFollowup(wasFollowup);
                    if (wasFollowup) {
                        session.setFollowupCount(session.getFollowupCount() + 1);
                    } else {
                        session.setFollowupCount(0);
                    }

                    session.getTempMessages().add(new MessageContext("assistant", response));

                    try {
                        Map<String, Object> endData = new HashMap<>();
                        if ("COMPLETED".equals(session.getStatus())) {
                            endData.put("message", "面试已结束，面试报告正在后端生成中");
                            endData.put("interviewEnded", true);
                        } else {
                            endData.put("questionCount", session.getQuestionCount());
                            endData.put("maxQuestions", session.getMaxQuestions());
                            endData.put("interviewEnded", false);
                        }
                        emitter.send(SseEmitter.event().name("end").data(endData));
                    } catch (Exception e) {}
                    emitter.complete();
                }

                @Override
                public void onError(Exception e) {
                    System.err.println("[Stream] Stream error: " + e.getMessage());
                    try {
                        emitter.send(SseEmitter.event().name("error").data("流式传输错误: " + e.getMessage()));
                    } catch (Exception ex) {}
                    emitter.complete();
                }
            });

        } catch (Exception e) {
            System.err.println("[Stream] Chat error: " + e.getMessage());
            try {
                emitter.send(SseEmitter.event().name("error").data("处理失败: " + e.getMessage()));
            } catch (Exception ex) {}
            emitter.complete();
        }
    }

    public Map<String, Object> voiceChat(MultipartFile audio, String sessionId, String token) throws Exception {
        Long userId = getUserIdFromToken(token);
        InterviewSession session = validateSession(sessionId, userId);

        if (!SESSION_TYPE_VOICE.equals(session.getSessionType())) {
            throw new RuntimeException("会话类型不匹配，请使用文字面试接口");
        }

        byte[] audioData = audio.getBytes();
        Map<String, Object> audioResult = audioFeatureService.processAudio(audioData);

        @SuppressWarnings("unchecked")
        Map<String, Object> audioFeatures = (Map<String, Object>) audioResult.get("audioFeatures");
        String transcript = (String) audioResult.get("transcript");

        Object durationObj = audioResult.get("duration");
        Double duration = 0.0;
        if (durationObj instanceof Number) {
            duration = ((Number) durationObj).doubleValue();
        }

        session.getTempMessages().add(new MessageContext(
            "user",
            transcript != null ? transcript : "[语音消息]",
            MessageContext.TYPE_VOICE,
            audioFeatures
        ));

        if (shouldAskFollowup(session)) {
            return generateVoiceFollowupResponse(session, sessionId, audioFeatures, duration, transcript);
        }

        if (shouldEndInterview(session)) {
            return generateVoiceEndResponse(session, sessionId, audioFeatures, duration, transcript);
        }

        return generateVoiceNewQuestionResponse(session, sessionId, audioFeatures, duration, transcript);
    }

    public Map<String, Object> startVoiceInterview(String sessionId, String token) throws Exception {
        Long userId = getUserIdFromToken(token);
        InterviewSession session = validateSession(sessionId, userId);

        if (!SESSION_TYPE_VOICE.equals(session.getSessionType())) {
            throw new RuntimeException("会话类型不匹配");
        }

        String aiResponse = aiService.generateNewQuestionResponse(
            session.getResumeText(),
            session.getPosition(),
            session.getTempMessages(),
            session.getUserId(),
            session.getInterviewType()
        );

        session.setQuestionCount(session.getQuestionCount() + 1);
        session.setLastWasFollowup(false);
        session.setFollowupCount(0);
        session.getTempMessages().add(new MessageContext("assistant", aiResponse, MessageContext.TYPE_VOICE));
        return buildVoiceResponse(sessionId, aiResponse, null, null, 0.0,
            session.getQuestionCount(), session.getMaxQuestions(), session.getFollowupCount(),
            session.getMaxFollowups(), false, false);
    }

    private Map<String, Object> generateVoiceFollowupResponse(InterviewSession session, String sessionId,
            Map<String, Object> audioFeatures, Double duration, String transcript) throws Exception {
        String aiResponse = aiService.generateFollowupResponse(
            session.getResumeText(),
            session.getPosition(),
            session.getTempMessages(),
            session.getFollowupCount(),
            session.getMaxFollowups(),
            session.getUserId(),
            session.getResumeId()
        );

        session.setFollowupCount(session.getFollowupCount() + 1);
        session.setLastWasFollowup(true);
        session.getTempMessages().add(new MessageContext("assistant", aiResponse, MessageContext.TYPE_VOICE));

        return buildVoiceResponse(sessionId, aiResponse, transcript, audioFeatures, duration,
            session.getQuestionCount(), session.getMaxQuestions(), session.getFollowupCount(), session.getMaxFollowups(), true, false);
    }

    private Map<String, Object> generateVoiceEndResponse(InterviewSession session, String sessionId,
            Map<String, Object> audioFeatures, Double duration, String transcript) {
        session.setStatus("COMPLETED");
        sessionRepository.save(session);
        Long dbSessionId = session.getId();
        saveVoiceMessages(session, audioFeatures, duration);

        if (!abilityEvaluationRepository.findBySessionId(session.getId()).isPresent()) {
            asyncEvaluationService.performEvaluationAsync(
                session.getId(),
                session.getSessionType(),
                session.getResumeText(),
                session.getPosition(),
                session.getUserId()
            );
        }

        activeSessions.remove(sessionId);

        Map<String, Object> data = buildVoiceResponseData(sessionId, "面试已结束，您已完成所有问题。",
            transcript, audioFeatures, duration,
            session.getQuestionCount(), session.getMaxQuestions(), session.getFollowupCount(), session.getMaxFollowups(), false, true);
        data.put("dbSessionId", dbSessionId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", data);
        return result;
    }

    private Map<String, Object> generateVoiceNewQuestionResponse(InterviewSession session, String sessionId,
            Map<String, Object> audioFeatures, Double duration, String transcript) throws Exception {
        String aiResponse = aiService.generateNewQuestionResponse(
            session.getResumeText(),
            session.getPosition(),
            session.getTempMessages(),
            session.getUserId(),
            session.getInterviewType()
        );

        session.setQuestionCount(session.getQuestionCount() + 1);
        session.setLastWasFollowup(false);
        session.setFollowupCount(0);
        session.getTempMessages().add(new MessageContext("assistant", aiResponse, MessageContext.TYPE_VOICE));

        return buildVoiceResponse(sessionId, aiResponse, transcript, audioFeatures, duration,
            session.getQuestionCount(), session.getMaxQuestions(), session.getFollowupCount(), session.getMaxFollowups(), false, false);
    }

    public List<Map<String, Object>> getHistory(Long sessionId, String token) throws Exception {
        Long userId = getUserIdFromToken(token);
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new Exception("会话不存在"));

        if (!session.getUserId().equals(userId)) {
            throw new Exception("无权访问此会话");
        }

        List<Map<String, Object>> history = new ArrayList<>();

        List<TextInterviewMessage> textMessages = textMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        for (TextInterviewMessage msg : textMessages) {
            history.add(Map.of(
                "role", msg.getRole(),
                "content", msg.getContent(),
                "messageType", "text",
                "time", msg.getCreatedAt().toString()
            ));
        }

        List<VoiceInterviewMessage> voiceMessages = voiceMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        for (VoiceInterviewMessage msg : voiceMessages) {
            history.add(Map.of(
                "role", msg.getRole(),
                "content", msg.getContent(),
                "messageType", "voice",
                "audioFeatures", Map.of(
                    "duration", msg.getDuration() != null ? msg.getDuration() : 0,
                    "speechRate", msg.getSpeechRate() != null ? msg.getSpeechRate() : 0,
                    "pauseCount", msg.getPauseCount() != null ? msg.getPauseCount() : 0,
                    "energyStability", msg.getEnergyStability() != null ? msg.getEnergyStability() : 0,
                    "pitchVariation", msg.getPitchVariation() != null ? msg.getPitchVariation() : 0,
                    "fillerWordCount", msg.getFillerWordCount() != null ? msg.getFillerWordCount() : 0,
                    "emotionTendency", msg.getEmotionTendency() != null ? msg.getEmotionTendency() : "unknown"
                ),
                "time", msg.getCreatedAt().toString()
            ));
        }

        return history;
    }

    public List<Map<String, Object>> getSessions(String token) throws Exception {
        Long userId = getUserIdFromToken(token);
        List<InterviewSession> sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (InterviewSession session : sessions) {
            if (!"COMPLETED".equals(session.getStatus())) {
                continue;
            }
            result.add(Map.of(
                "id", session.getId(),
                "position", session.getPosition(),
                "status", session.getStatus(),
                "sessionType", session.getSessionType(),
                "createdAt", session.getCreatedAt().toString(),
                "questionCount", session.getQuestionCount() != null ? session.getQuestionCount() : 0,
                "maxQuestions", session.getMaxQuestions() != null ? session.getMaxQuestions() : 10,
                "followupCount", session.getFollowupCount() != null ? session.getFollowupCount() : 0,
                "maxFollowups", session.getMaxFollowups() != null ? session.getMaxFollowups() : 3
            ));
        }
        return result;
    }

    public List<Map<String, Object>> getEvaluationHistory(String token) throws Exception {
        Long userId = getUserIdFromToken(token);
        List<AbilityEvaluation> evaluations = abilityEvaluationRepository.findByUserIdOrderByCreatedAtDesc(userId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (AbilityEvaluation eval : evaluations) {
            Map<String, Object> item = new HashMap<>();
            item.put("evaluationId", eval.getId());
            item.put("sessionId", eval.getSessionId());
            item.put("sessionType", eval.getSessionType());
            item.put("position", eval.getPosition());
            item.put("overallScore", eval.getOverallScore());
            item.put("technicalCorrectness", eval.getTechnicalCorrectness());
            item.put("knowledgeDepth", eval.getKnowledgeDepth());
            item.put("logicRigorous", eval.getLogicRigorous());
            item.put("speechRateScore", eval.getSpeechRateScore());
            item.put("clarityScore", eval.getClarityScore());
            item.put("confidenceScore", eval.getConfidenceScore());
            item.put("strengths", eval.getStrengths());
            item.put("weaknesses", eval.getWeaknesses());
            item.put("improvementSuggestions", eval.getImprovementSuggestions());
            item.put("createdAt", eval.getCreatedAt() != null ? eval.getCreatedAt().toString() : "");

            InterviewSession session = sessionRepository.findById(eval.getSessionId()).orElse(null);
            if (session != null) {
                item.put("questionCount", session.getQuestionCount());
                item.put("followupCount", session.getFollowupCount());
            }

            result.add(item);
        }
        return result;
    }

    public Map<String, Object> getAbilityGrowthCurve(String token) throws Exception {
        Long userId = getUserIdFromToken(token);
        List<AbilityEvaluation> evaluations = abilityEvaluationRepository.findByUserIdOrderByCreatedAtDesc(userId);

        List<AbilityEvaluation> voiceEvaluations = new ArrayList<>();
        for (AbilityEvaluation eval : evaluations) {
            if ("VOICE".equals(eval.getSessionType())) {
                voiceEvaluations.add(eval);
            }
        }

        List<Map<String, Object>> overallTrend = new ArrayList<>();
        List<Map<String, Object>> technicalTrend = new ArrayList<>();
        List<Map<String, Object>> knowledgeTrend = new ArrayList<>();
        List<Map<String, Object>> logicTrend = new ArrayList<>();
        List<Map<String, Object>> speechTrend = new ArrayList<>();
        List<Map<String, Object>> clarityTrend = new ArrayList<>();
        List<Map<String, Object>> confidenceTrend = new ArrayList<>();

        int totalCount = evaluations.size();
        int voiceCount = voiceEvaluations.size();

        for (int i = evaluations.size() - 1; i >= 0; i--) {
            AbilityEvaluation eval = evaluations.get(i);
            Map<String, Object> point = new HashMap<>();
            point.put("index", evaluations.size() - i);
            point.put("date", eval.getCreatedAt() != null ? eval.getCreatedAt().toString().substring(0, 10) : "");
            point.put("position", eval.getPosition());

            overallTrend.add(Map.of(
                "index", evaluations.size() - i,
                "date", eval.getCreatedAt() != null ? eval.getCreatedAt().toString().substring(0, 10) : "",
                "score", eval.getOverallScore() != null ? eval.getOverallScore() : 0
            ));

            technicalTrend.add(Map.of(
                "index", evaluations.size() - i,
                "date", eval.getCreatedAt() != null ? eval.getCreatedAt().toString().substring(0, 10) : "",
                "score", eval.getTechnicalCorrectness() != null ? eval.getTechnicalCorrectness() : 0
            ));

            knowledgeTrend.add(Map.of(
                "index", evaluations.size() - i,
                "date", eval.getCreatedAt() != null ? eval.getCreatedAt().toString().substring(0, 10) : "",
                "score", eval.getKnowledgeDepth() != null ? eval.getKnowledgeDepth() : 0
            ));

            logicTrend.add(Map.of(
                "index", evaluations.size() - i,
                "date", eval.getCreatedAt() != null ? eval.getCreatedAt().toString().substring(0, 10) : "",
                "score", eval.getLogicRigorous() != null ? eval.getLogicRigorous() : 0
            ));
        }

        for (int i = voiceEvaluations.size() - 1; i >= 0; i--) {
            AbilityEvaluation eval = voiceEvaluations.get(i);

            if (eval.getSpeechRateScore() != null) {
                speechTrend.add(Map.of(
                    "index", voiceEvaluations.size() - i,
                    "date", eval.getCreatedAt() != null ? eval.getCreatedAt().toString().substring(0, 10) : "",
                    "score", eval.getSpeechRateScore()
                ));
            }

            if (eval.getClarityScore() != null) {
                clarityTrend.add(Map.of(
                    "index", voiceEvaluations.size() - i,
                    "date", eval.getCreatedAt() != null ? eval.getCreatedAt().toString().substring(0, 10) : "",
                    "score", eval.getClarityScore()
                ));
            }

            if (eval.getConfidenceScore() != null) {
                confidenceTrend.add(Map.of(
                    "index", voiceEvaluations.size() - i,
                    "date", eval.getCreatedAt() != null ? eval.getCreatedAt().toString().substring(0, 10) : "",
                    "score", eval.getConfidenceScore()
                ));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("overallTrend", overallTrend);
        result.put("technicalTrend", technicalTrend);
        result.put("knowledgeTrend", knowledgeTrend);
        result.put("logicTrend", logicTrend);
        result.put("speechTrend", speechTrend);
        result.put("clarityTrend", clarityTrend);
        result.put("confidenceTrend", confidenceTrend);
        result.put("totalEvaluations", evaluations.size());
        result.put("voiceInterviewCount", voiceCount);

        if (totalCount >= 2) {
            int halfCount = totalCount / 2;
            List<AbilityEvaluation> firstHalf = evaluations.subList(totalCount - halfCount, totalCount);
            List<AbilityEvaluation> secondHalf = evaluations.subList(0, halfCount);

            Map<String, Object> improvement = new HashMap<>();
            improvement.put("overallChange", calculateAverageImprovement(firstHalf, secondHalf, "getOverallScore"));
            improvement.put("technicalChange", calculateAverageImprovement(firstHalf, secondHalf, "getTechnicalCorrectness"));
            improvement.put("knowledgeChange", calculateAverageImprovement(firstHalf, secondHalf, "getKnowledgeDepth"));
            improvement.put("logicChange", calculateAverageImprovement(firstHalf, secondHalf, "getLogicRigorous"));

            result.put("improvement", improvement);
            result.put("comparisonInfo", Map.of(
                "firstHalfCount", halfCount,
                "secondHalfCount", halfCount,
                "firstHalfLabel", "早期",
                "secondHalfLabel", "近期"
            ));
        } else if (totalCount == 1) {
            Map<String, Object> improvement = new HashMap<>();
            improvement.put("overallChange", 0);
            improvement.put("technicalChange", 0);
            improvement.put("knowledgeChange", 0);
            improvement.put("logicChange", 0);

            result.put("improvement", improvement);
        }

        if (voiceCount >= 2) {
            int voiceHalfCount = voiceCount / 2;
            List<AbilityEvaluation> voiceFirstHalf = voiceEvaluations.subList(voiceCount - voiceHalfCount, voiceCount);
            List<AbilityEvaluation> voiceSecondHalf = voiceEvaluations.subList(0, voiceHalfCount);

            Map<String, Object> voiceImprovement = new HashMap<>();
            voiceImprovement.put("speechRateChange", calculateAverageImprovement(voiceFirstHalf, voiceSecondHalf, "getSpeechRateScore"));
            voiceImprovement.put("clarityChange", calculateAverageImprovement(voiceFirstHalf, voiceSecondHalf, "getClarityScore"));
            voiceImprovement.put("confidenceChange", calculateAverageImprovement(voiceFirstHalf, voiceSecondHalf, "getConfidenceScore"));

            Map<String, Object> voiceStats = new HashMap<>();
            voiceStats.put("speechRateAvg", calculateAverageScore(voiceSecondHalf, "getSpeechRateScore"));
            voiceStats.put("clarityAvg", calculateAverageScore(voiceSecondHalf, "getClarityScore"));
            voiceStats.put("confidenceAvg", calculateAverageScore(voiceSecondHalf, "getConfidenceScore"));

            result.put("voiceImprovement", voiceImprovement);
            result.put("voiceStats", voiceStats);
            result.put("hasVoiceData", true);
        } else if (voiceCount == 1) {
            AbilityEvaluation eval = voiceEvaluations.get(0);
            Map<String, Object> voiceImprovement = new HashMap<>();
            voiceImprovement.put("speechRateChange", 0);
            voiceImprovement.put("clarityChange", 0);
            voiceImprovement.put("confidenceChange", 0);

            Map<String, Object> voiceStats = new HashMap<>();
            voiceStats.put("speechRateAvg", eval.getSpeechRateScore());
            voiceStats.put("clarityAvg", eval.getClarityScore());
            voiceStats.put("confidenceAvg", eval.getConfidenceScore());

            result.put("voiceImprovement", voiceImprovement);
            result.put("voiceStats", voiceStats);
            result.put("hasVoiceData", eval.getSpeechRateScore() != null || eval.getClarityScore() != null || eval.getConfidenceScore() != null);
        } else {
            result.put("hasVoiceData", false);
        }

        return result;
    }

    private double calculateAverageImprovement(List<AbilityEvaluation> firstHalf, List<AbilityEvaluation> secondHalf, String methodName) {
        Double firstAvg = calculateAverageScore(firstHalf, methodName);
        Double secondAvg = calculateAverageScore(secondHalf, methodName);
        if (firstAvg == null || secondAvg == null || firstAvg == 0) return 0;
        return Math.round((secondAvg - firstAvg) * 100.0) / 100.0;
    }

    private double calculateAverageScore(List<AbilityEvaluation> evaluations, String methodName) {
        double sum = 0;
        int count = 0;
        for (AbilityEvaluation eval : evaluations) {
            Double score = getScoreFromEvaluation(eval, methodName);
            if (score != null) {
                sum += score;
                count++;
            }
        }
        return count > 0 ? sum / count : 0;
    }

    private Double getScoreFromEvaluation(AbilityEvaluation eval, String methodName) {
        switch (methodName) {
            case "getOverallScore": return eval.getOverallScore();
            case "getTechnicalCorrectness": return eval.getTechnicalCorrectness();
            case "getKnowledgeDepth": return eval.getKnowledgeDepth();
            case "getLogicRigorous": return eval.getLogicRigorous();
            case "getSpeechRateScore": return eval.getSpeechRateScore();
            case "getClarityScore": return eval.getClarityScore();
            case "getConfidenceScore": return eval.getConfidenceScore();
            default: return null;
        }
    }

    public Map<String, Object> getDebugEvaluationData(String token) throws Exception {
        Long userId = getUserIdFromToken(token);
        List<AbilityEvaluation> evaluations = abilityEvaluationRepository.findByUserIdOrderByCreatedAtDesc(userId);

        Map<String, Object> debugInfo = new HashMap<>();
        debugInfo.put("userId", userId);
        debugInfo.put("evaluationCount", evaluations.size());
        debugInfo.put("hasEvaluations", !evaluations.isEmpty());

        if (!evaluations.isEmpty()) {
            List<Map<String, Object>> evaluationSummaries = new ArrayList<>();
            for (AbilityEvaluation eval : evaluations) {
                Map<String, Object> summary = new HashMap<>();
                summary.put("id", eval.getId());
                summary.put("sessionId", eval.getSessionId());
                summary.put("position", eval.getPosition());
                summary.put("overallScore", eval.getOverallScore());
                summary.put("createdAt", eval.getCreatedAt());
                evaluationSummaries.add(summary);
            }
            debugInfo.put("evaluations", evaluationSummaries);
        }

        List<InterviewSession> sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        debugInfo.put("sessionCount", sessions.size());

        List<Map<String, Object>> sessionSummaries = new ArrayList<>();
        for (InterviewSession session : sessions) {
            Map<String, Object> summary = new HashMap<>();
            summary.put("id", session.getId());
            summary.put("status", session.getStatus());
            summary.put("position", session.getPosition());
            summary.put("hasEvaluation", abilityEvaluationRepository.findBySessionId(session.getId()).isPresent());
            sessionSummaries.add(summary);
        }
        debugInfo.put("sessions", sessionSummaries);

        return debugInfo;
    }

    public Map<String, Object> getEvaluationBySession(Long sessionId, String token) throws Exception {
        Long userId = getUserIdFromToken(token);
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new Exception("会话不存在"));

        if (!session.getUserId().equals(userId)) {
            throw new Exception("无权访问此评估");
        }

        AbilityEvaluation evaluation = abilityEvaluationRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new Exception("该面试暂无评估结果"));

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", buildEvaluationDetail(evaluation, session));

        return result;
    }

    private Map<String, Object> buildEvaluationDetail(AbilityEvaluation eval, InterviewSession session) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("evaluationId", eval.getId());
        detail.put("sessionId", eval.getSessionId());
        detail.put("sessionType", eval.getSessionType());
        detail.put("position", eval.getPosition());
        detail.put("overallScore", eval.getOverallScore());
        detail.put("overallGrade", getScoreGrade(eval.getOverallScore()));

        Map<String, Object> dimensions = new HashMap<>();
        dimensions.put("technicalCorrectness", Map.of(
            "score", eval.getTechnicalCorrectness() != null ? eval.getTechnicalCorrectness() : 0,
            "grade", getScoreGrade(eval.getTechnicalCorrectness()),
            "detail", eval.getTechnicalCorrectnessDetail() != null ? eval.getTechnicalCorrectnessDetail() : ""
        ));
        dimensions.put("knowledgeDepth", Map.of(
            "score", eval.getKnowledgeDepth() != null ? eval.getKnowledgeDepth() : 0,
            "grade", getScoreGrade(eval.getKnowledgeDepth()),
            "detail", eval.getKnowledgeDepthDetail() != null ? eval.getKnowledgeDepthDetail() : ""
        ));
        dimensions.put("logicRigorous", Map.of(
            "score", eval.getLogicRigorous() != null ? eval.getLogicRigorous() : 0,
            "grade", getScoreGrade(eval.getLogicRigorous()),
            "detail", eval.getLogicRigorousDetail() != null ? eval.getLogicRigorousDetail() : ""
        ));

        if (eval.getSpeechRateScore() != null) {
            dimensions.put("speechRate", Map.of(
                "score", eval.getSpeechRateScore(),
                "grade", getScoreGrade(eval.getSpeechRateScore()),
                "detail", eval.getExpressionDetail() != null ? eval.getExpressionDetail() : ""
            ));
        }
        if (eval.getClarityScore() != null) {
            dimensions.put("clarity", Map.of(
                "score", eval.getClarityScore(),
                "grade", getScoreGrade(eval.getClarityScore()),
                "detail", ""
            ));
        }
        if (eval.getConfidenceScore() != null) {
            dimensions.put("confidence", Map.of(
                "score", eval.getConfidenceScore(),
                "grade", getScoreGrade(eval.getConfidenceScore()),
                "detail", ""
            ));
        }

        detail.put("dimensions", dimensions);
        detail.put("strengths", eval.getStrengths() != null ? eval.getStrengths() : "");
        detail.put("weaknesses", eval.getWeaknesses() != null ? eval.getWeaknesses() : "");
        detail.put("improvementSuggestions", eval.getImprovementSuggestions() != null ? eval.getImprovementSuggestions() : "");

        String fullAnalysisText = extractFullAnalysisText(eval.getFullAnalysis());
        detail.put("fullAnalysis", fullAnalysisText);
        detail.put("questionCount", session.getQuestionCount());
        detail.put("followupCount", session.getFollowupCount());
        detail.put("createdAt", eval.getCreatedAt() != null ? eval.getCreatedAt().toString() : "");

        return detail;
    }

    private String extractFullAnalysisText(String fullAnalysis) {
        if (fullAnalysis == null || fullAnalysis.isEmpty()) return "";
        try {
            String jsonStr = extractJsonFromText(fullAnalysis);
            if (jsonStr != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>> typeRef =
                    new com.fasterxml.jackson.core.type.TypeReference<>() {};
                Map<String, Object> json = mapper.readValue(jsonStr, typeRef);
                Object fa = json.get("fullAnalysis");
                if (fa != null) return fa.toString();
                Object finalSummary = json.get("FinalSummary");
                if (finalSummary != null) return finalSummary.toString();
            }
        } catch (Exception e) {
        }
        return fullAnalysis;
    }

    private String getScoreGrade(Double score) {
        if (score == null) return "暂无评分";
        if (score >= 90) return "优秀";
        if (score >= 75) return "良好";
        if (score >= 60) return "及格";
        return "需提升";
    }

    @Transactional
    public void deleteSession(Long sessionId, String token) throws Exception {
        Long userId = getUserIdFromToken(token);
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new Exception("会话不存在"));

        if (!session.getUserId().equals(userId)) {
            throw new Exception("无权删除此会话");
        }

        abilityEvaluationRepository.deleteBySessionId(sessionId);
        textMessageRepository.deleteBySessionId(sessionId);
        voiceMessageRepository.deleteBySessionId(sessionId);
        sessionRepository.delete(session);
    }

    private void saveTextMessages(InterviewSession session) {
        Long dbSessionId = session.getId();
        for (MessageContext msg : session.getTempMessages()) {
            if (msg.getContent() == null || msg.getContent().isEmpty()) continue;
            if (msg.isVoice()) continue;
            TextInterviewMessage entity = new TextInterviewMessage();
            entity.setSessionId(dbSessionId);
            entity.setRole(msg.getRole());
            entity.setContent(msg.getContent());
            textMessageRepository.save(entity);
        }
    }

    private void saveVoiceMessages(InterviewSession session, Map<String, Object> audioFeatures, Double duration) {
        Long dbSessionId = session.getId();
        for (MessageContext msg : session.getTempMessages()) {
            if (msg.getContent() == null || msg.getContent().isEmpty()) continue;
            if (!msg.isVoice()) continue;
            VoiceInterviewMessage entity = new VoiceInterviewMessage();
            entity.setSessionId(dbSessionId);
            entity.setRole(msg.getRole());
            entity.setContent(msg.getContent());
            entity.setDuration(duration);
            entity.setSpeechRate(getIntValue(audioFeatures, "speechRate"));
            entity.setPauseCount(getIntValue(audioFeatures, "pauseCount"));
            entity.setEnergyStability(getDoubleValue(audioFeatures, "energyStability"));
            entity.setPitchVariation(getDoubleValue(audioFeatures, "pitchVariation"));
            entity.setFillerWordCount(getIntValue(audioFeatures, "fillerWordCount"));
            entity.setEmotionTendency((String) audioFeatures.getOrDefault("emotionTendency", "unknown"));
            voiceMessageRepository.save(entity);
        }
    }

    private Integer getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    public Map<String, Object> evaluateAbility(Long sessionId, String token) throws Exception {
        Long userId = getUserIdFromToken(token);
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new Exception("会话不存在"));

        if (!session.getUserId().equals(userId)) {
            throw new Exception("无权访问此会话");
        }

        if (abilityEvaluationRepository.findBySessionId(sessionId).isPresent()) {
            throw new Exception("该面试已完成能力评估");
        }

        AbilityEvaluation evaluation = performAbilityEvaluation(session);
        abilityEvaluationRepository.save(evaluation);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);

        Map<String, Object> data = new HashMap<>();
        data.put("evaluationId", evaluation.getId());
        data.put("overallScore", evaluation.getOverallScore());
        data.put("technicalCorrectness", evaluation.getTechnicalCorrectness());
        data.put("knowledgeDepth", evaluation.getKnowledgeDepth());
        data.put("logicRigorous", evaluation.getLogicRigorous());
        data.put("speechRateScore", evaluation.getSpeechRateScore());
        data.put("clarityScore", evaluation.getClarityScore());
        data.put("confidenceScore", evaluation.getConfidenceScore());
        data.put("strengths", evaluation.getStrengths());
        data.put("weaknesses", evaluation.getWeaknesses());
        data.put("improvementSuggestions", evaluation.getImprovementSuggestions());
        data.put("fullAnalysis", evaluation.getFullAnalysis());
        result.put("data", data);

        return result;
    }

    private AbilityEvaluation performAbilityEvaluation(InterviewSession session) throws Exception {
        Long sessionId = session.getId();
        String sessionType = session.getSessionType();
        StringBuilder conversation = new StringBuilder();

        if (SESSION_TYPE_TEXT.equals(sessionType)) {
            List<TextInterviewMessage> textMessages = textMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
            for (TextInterviewMessage msg : textMessages) {
                String role = msg.getRole().equals("user") ? "面试者" : "面试官";
                conversation.append(role).append("：").append(msg.getContent()).append("\n");
            }
        } else {
            List<VoiceInterviewMessage> voiceMessages = voiceMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
            for (VoiceInterviewMessage msg : voiceMessages) {
                String role = msg.getRole().equals("user") ? "面试者" : "面试官";
                conversation.append(role).append("：").append(msg.getContent()).append("\n");
            }
        }

        String evaluationPrompt;
        if (SESSION_TYPE_TEXT.equals(sessionType)) {
            evaluationPrompt = buildTextEvaluationPrompt(session, conversation.toString());
        } else {
            List<VoiceInterviewMessage> voiceMessages = voiceMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
            evaluationPrompt = buildVoiceEvaluationPrompt(session, conversation.toString(), voiceMessages);
        }

        System.out.printf("===== EVALUATION PROMPT START =====\n%s\n===== EVALUATION PROMPT END =====\n", evaluationPrompt);

        String evaluationResponse = aiService.generateAbilityEvaluation(evaluationPrompt);
        return parseEvaluationResult(session, evaluationResponse, sessionType);
    }

    private String buildTextEvaluationPrompt(InterviewSession session, String conversation) {
        String structuredQA = structureConversationAsQAPairs(conversation);

        String excellentAnswersContext = "";
        try {
            excellentAnswersContext = excellentAnswerService.retrieveRelevantExcellentAnswers(
                session.getPosition() + " " + session.getResumeText(), 3
            );
        } catch (Exception e) {
            System.err.println("Failed to retrieve excellent answers for evaluation: " + e.getMessage());
        }

        String excellentPrompt = "";
        if (!excellentAnswersContext.isEmpty()) {
            excellentPrompt = """
                【优秀回答参考】
                以下是类似问题的优秀回答示例，评分时可作为参考标准：
                %s

                """.formatted(excellentAnswersContext);
        }

        return String.format("""
            面试类型：文字面试
            目标岗位：%s
            简历内容：%s

            %s

            【重要说明】
            以下是面试中的问答记录。请注意：
            1. 问题由面试官提出，可能包含技术关键词和考察点提示
            2. 你的评价应该**只看面试者的回答内容本身**
            3. 不要因为问题包含提示就认为面试者回答得好或差
            4. 重点评估面试者回答的准确性、深度和逻辑性

            面试问答记录：
            %s

            请对面试者的回答进行深度能力分析，评估以下方面：
            1. 技术正确性：回答中的技术知识是否准确
            2. 知识深度：对技术问题的理解深度和广度
            3. 逻辑严谨性：回答的逻辑是否清晰、严谨

            请按以下JSON格式返回分析结果：
            {
                "overallScore": 综合评分(0-100),
                "technicalCorrectness": 技术正确性评分(0-100),
                "technicalCorrectnessDetail": 技术正确性详细分析,
                "knowledgeDepth": 知识深度评分(0-100),
                "knowledgeDepthDetail": 知识深度详细分析,
                "logicRigorous": 逻辑严谨性评分(0-100),
                "logicRigorousDetail": 逻辑严谨性详细分析,
                "strengths": 主要优势,
                "weaknesses": 主要不足,
                "improvementSuggestions": 改进建议,
                "fullAnalysis": 完整深度分析报告
            }

            只返回JSON格式，不要其他内容。
            """, session.getPosition(), session.getResumeText(), excellentPrompt, structuredQA);
    }
    
    private String structureConversationAsQAPairs(String conversation) {
        StringBuilder structured = new StringBuilder();
        String[] lines = conversation.split("\n");
        String lastQuestion = "";
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            
            if (line.startsWith("面试官：")) {
                lastQuestion = line.substring(4);
            } else if (line.startsWith("面试者：") && !lastQuestion.isEmpty()) {
                structured.append("【问】").append(lastQuestion).append("\n");
                structured.append("【答】").append(line.substring(4)).append("\n\n");
                lastQuestion = "";
            }
        }
        
        return structured.length() > 0 ? structured.toString() : conversation;
    }

    private String buildVoiceEvaluationPrompt(InterviewSession session, String conversation, List<VoiceInterviewMessage> voiceMessages) {
        String structuredQA = structureConversationAsQAPairs(conversation);

        String excellentAnswersContext = "";
        try {
            excellentAnswersContext = excellentAnswerService.retrieveRelevantExcellentAnswers(
                session.getPosition() + " " + session.getResumeText(), 3
            );
        } catch (Exception e) {
            System.err.println("Failed to retrieve excellent answers for evaluation: " + e.getMessage());
        }

        String excellentPrompt = "";
        if (!excellentAnswersContext.isEmpty()) {
            excellentPrompt = """
                【优秀回答参考】
                以下是类似问题的优秀回答示例，评分时可作为参考标准：
                %s

                """.formatted(excellentAnswersContext);
        }

        StringBuilder voiceAnalysis = new StringBuilder();
        for (VoiceInterviewMessage msg : voiceMessages) {
            if ("user".equals(msg.getRole())) {
                voiceAnalysis.append(String.format("""
                    - 语速: %d 字/分钟
                    - 停顿次数: %d
                    - 能量稳定性: %.2f
                    - 音调变化: %.2f
                    - 填充词数量: %d
                    - 情感倾向: %s
                    """,
                    msg.getSpeechRate() != null ? msg.getSpeechRate() : 0,
                    msg.getPauseCount() != null ? msg.getPauseCount() : 0,
                    msg.getEnergyStability() != null ? msg.getEnergyStability() : 0.0,
                    msg.getPitchVariation() != null ? msg.getPitchVariation() : 0.0,
                    msg.getFillerWordCount() != null ? msg.getFillerWordCount() : 0,
                    msg.getEmotionTendency() != null ? msg.getEmotionTendency() : "unknown"
                ));
            }
        }

        return String.format("""
            面试类型：语音面试
            目标岗位：%s
            简历内容：%s

            %s

            【重要说明】
            以下是面试中的问答记录。请注意：
            1. 问题由面试官提出，可能包含技术关键词和考察点提示
            2. 你的评价应该**只看面试者的回答内容本身**
            3. 不要因为问题包含提示就认为面试者回答得好或差
            4. 重点评估面试者回答的准确性、深度和逻辑性

            面试问答记录：
            %s

            语音特征分析：
            %s

            请对面试者的回答进行深度能力分析，评估以下方面：

            【内容能力评估】
            1. 技术正确性：回答中的技术知识是否准确
            2. 知识深度：对技术问题的理解深度和广度
            3. 逻辑严谨性：回答的逻辑是否清晰、严谨

            【表达能力评估】
            4. 语速评分：语速是否适中（120-160字/分钟为最佳）
            5. 清晰度：表达是否清晰有条理
            6. 自信度：从表达中体现的自信程度

            请按以下JSON格式返回分析结果：
            {
                "overallScore": 综合评分(0-100),
                "technicalCorrectness": 技术正确性评分(0-100),
                "technicalCorrectnessDetail": 技术正确性详细分析,
                "knowledgeDepth": 知识深度评分(0-100),
                "knowledgeDepthDetail": 知识深度详细分析,
                "logicRigorous": 逻辑严谨性评分(0-100),
                "logicRigorousDetail": 逻辑严谨性详细分析,
                "speechRateScore": 语速评分(0-100),
                "clarityScore": 清晰度评分(0-100),
                "confidenceScore": 自信度评分(0-100),
                "expressionDetail": 表达能力详细分析,
                "strengths": 主要优势,
                "weaknesses": 主要不足,
                "improvementSuggestions": 改进建议,
                "fullAnalysis": 完整深度分析报告
            }

            只返回JSON格式，不要其他内容。
            """, session.getPosition(), session.getResumeText(), excellentPrompt, structuredQA, voiceAnalysis.toString());
    }

    private AbilityEvaluation parseEvaluationResult(InterviewSession session, String evaluationResponse, String sessionType) {
        AbilityEvaluation evaluation = new AbilityEvaluation();
        evaluation.setSessionId(session.getId());
        evaluation.setSessionType(sessionType);
        evaluation.setUserId(session.getUserId());
        evaluation.setPosition(session.getPosition());
        evaluation.setFullAnalysis(evaluationResponse);

        try {
            String jsonStr = extractJsonFromText(evaluationResponse);
            if (jsonStr != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>> typeRef =
                    new com.fasterxml.jackson.core.type.TypeReference<>() {};
                Map<String, Object> json = mapper.readValue(jsonStr, typeRef);

                evaluation.setOverallScore(parseDouble(json.get("overallScore")));
                evaluation.setTechnicalCorrectness(parseDouble(json.get("technicalCorrectness")));
                evaluation.setTechnicalCorrectnessDetail(parseDetailValue(json.get("technicalCorrectnessDetail")));
                evaluation.setKnowledgeDepth(parseDouble(json.get("knowledgeDepth")));
                evaluation.setKnowledgeDepthDetail(parseDetailValue(json.get("knowledgeDepthDetail")));
                evaluation.setLogicRigorous(parseDouble(json.get("logicRigorous")));
                evaluation.setLogicRigorousDetail(parseDetailValue(json.get("logicRigorousDetail")));
                evaluation.setSpeechRateScore(parseDouble(json.get("speechRateScore")));
                evaluation.setClarityScore(parseDouble(json.get("clarityScore")));
                evaluation.setConfidenceScore(parseDouble(json.get("confidenceScore")));
                evaluation.setExpressionDetail(parseDetailValue(json.get("expressionDetail")));
                evaluation.setStrengths(parseStringOrArray(json.get("strengths")));
                evaluation.setWeaknesses(parseStringOrArray(json.get("weaknesses")));
                evaluation.setImprovementSuggestions(parseStringOrArray(json.get("improvementSuggestions")));
            } else {
                setEvaluationDefaults(evaluation);
            }
        } catch (Exception e) {
            setEvaluationDefaults(evaluation);
        }

        return evaluation;
    }

    private String extractJsonFromText(String text) {
        if (text == null) return null;
        text = text.trim();

        int jsonStart = -1;
        int jsonEnd = -1;

        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '{') {
                jsonStart = i;
                break;
            }
        }

        if (jsonStart == -1) return null;

        int braceCount = 0;
        boolean inString = false;
        for (int i = jsonStart; i < text.length(); i++) {
            char c = text.charAt(i);
            char prev = (i > 0) ? text.charAt(i - 1) : 0;

            if (c == '"' && prev != '\\') {
                inString = !inString;
            } else if (!inString) {
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        jsonEnd = i + 1;
                        break;
                    }
                }
            }
        }

        if (jsonStart != -1 && jsonEnd != -1) {
            return text.substring(jsonStart, jsonEnd);
        }
        return null;
    }

    private void setEvaluationDefaults(AbilityEvaluation evaluation) {
        evaluation.setOverallScore(0.0);
        evaluation.setTechnicalCorrectness(0.0);
        evaluation.setTechnicalCorrectnessDetail("解析失败");
        evaluation.setKnowledgeDepth(0.0);
        evaluation.setKnowledgeDepthDetail("解析失败");
        evaluation.setLogicRigorous(0.0);
        evaluation.setLogicRigorousDetail("解析失败");
        evaluation.setStrengths("数据解析失败，请稍后重试");
        evaluation.setWeaknesses("数据解析失败");
        evaluation.setImprovementSuggestions("数据解析失败");
    }

    private Double parseDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String parseStringOrArray(Object value) {
        if (value == null) return "";
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append("\n");
                sb.append(i + 1).append(". ").append(list.get(i).toString());
            }
            return sb.toString();
        }
        return value.toString();
    }

    private String parseDetailValue(Object value) {
        if (value == null) return "";
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append("\n");
                sb.append(list.get(i).toString());
            }
            return sb.toString();
        }
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (i > 0) sb.append("\n");
                sb.append("- ").append(entry.getKey()).append(": ").append(entry.getValue());
                i++;
            }
            return sb.toString();
        }
        return value.toString();
    }

    private String extractResumeText(MultipartFile file, Long resumeId, Long userId) throws Exception {
        if (file != null) {
            return aiService.extractText(file);
        } else if (resumeId != null) {
            Resume resume = resumeRepository.findById(resumeId)
                    .orElseThrow(() -> new Exception("简历不存在"));
            if (!resume.getUserId().equals(userId)) {
                throw new Exception("无权访问此简历");
            }
            return buildResumeTextFromDb(resume);
        } else {
            Optional<Resume> userResume = resumeRepository.findByUserId(userId);
            if (userResume.isPresent()) {
                return buildResumeTextFromDb(userResume.get());
            }
            throw new Exception("请提供简历文件或简历ID");
        }
    }

    private String buildResumeTextFromDb(Resume resume) {
        StringBuilder sb = new StringBuilder();
        if (resume.getName() != null && !resume.getName().isEmpty()) {
            sb.append("姓名：").append(resume.getName()).append("\n");
        }
        if (resume.getGender() != null && !resume.getGender().isEmpty()) {
            sb.append("性别：").append(resume.getGender()).append("\n");
        }
        if (resume.getAge() != null) {
            sb.append("年龄：").append(resume.getAge()).append("\n");
        }
        if (resume.getEducation() != null && !resume.getEducation().isEmpty()) {
            sb.append("学历：").append(resume.getEducation()).append("\n");
        }
        if (resume.getPhone() != null && !resume.getPhone().isEmpty()) {
            sb.append("电话：").append(resume.getPhone()).append("\n");
        }
        if (resume.getEmail() != null && !resume.getEmail().isEmpty()) {
            sb.append("邮箱：").append(resume.getEmail()).append("\n");
        }
        if (resume.getPosition() != null && !resume.getPosition().isEmpty()) {
            sb.append("应聘岗位：").append(resume.getPosition()).append("\n");
        }
        if (resume.getExperience() != null && !resume.getExperience().isEmpty()) {
            sb.append("工作经历：\n").append(resume.getExperience()).append("\n");
        }
        if (resume.getSkills() != null && !resume.getSkills().isEmpty()) {
            sb.append("技能：").append(resume.getSkills()).append("\n");
        }
        return sb.toString();
    }

    private void validatePositionMatch(String resumeText, String position) throws Exception {
        String matchResult = aiService.checkPositionMatch(resumeText, position);
        if (!"MATCH".equals(matchResult) && !"PARTIAL".equals(matchResult)) {
            throw new Exception("简历与所选岗位不匹配：" + matchResult);
        }
    }

    private InterviewSession createSession(Long userId, String position, Long resumeId, String resumeText, String sessionType, String interviewType) {
        InterviewSession session = new InterviewSession();
        session.setUserId(userId);
        session.setPosition(position);
        session.setResumeId(resumeId);
        session.setResumeText(resumeText);
        session.setSessionType(sessionType);
        session.setInterviewType(interviewType);
        session.setStatus("ACTIVE");
        session.setQuestionCount(0);
        session.setFollowupCount(0);

        if ("TECH".equals(interviewType)) {
            session.setMaxQuestions(techMaxQuestions);
            session.setMaxFollowups(techMaxFollowups);
        } else {
            session.setMaxQuestions(projectMaxQuestions);
            session.setMaxFollowups(projectMaxFollowups);
        }
        session.setTempMessages(new ArrayList<>());
        return session;
    }

    private InterviewSession validateSession(String sessionId, Long userId) {
        if (sessionId == null || sessionId.isEmpty()) {
            throw new RuntimeException("sessionId不能为空，请先开始面试");
        }
        InterviewSession session = activeSessions.get(sessionId);
        if (session == null) {
            throw new RuntimeException("无效的面试会话，请重新开始面试");
        }
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问此会话");
        }
        if ("COMPLETED".equals(session.getStatus())) {
            throw new RuntimeException("面试已结束");
        }
        session.setLastActiveTime(System.currentTimeMillis());
        return session;
    }

    private Long getUserIdFromToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            throw new RuntimeException("无效的用户令牌");
        }
        return userId;
    }

    private Map<String, Object> buildSuccessResponse(InterviewSession session, String message) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", session.getId().toString());
        data.put("message", message);
        data.put("maxQuestions", session.getMaxQuestions() != null ? session.getMaxQuestions() : 10);
        data.put("maxFollowups", session.getMaxFollowups() != null ? session.getMaxFollowups() : 3);
        result.put("success", true);
        result.put("data", data);
        return result;
    }

    private Map<String, Object> buildChatResponse(String sessionId, InterviewSession session,
            String aiResponse, boolean isFollowup, boolean interviewEnded) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);

        String finalResponse = aiResponse;
        if (interviewEnded) {
            finalResponse += "\n\n【面试已结束】感谢您的参与，您已经回答了" + session.getQuestionCount() + "个主要问题。";
        }

        result.put("data", buildChatResponseData(sessionId, session, finalResponse, isFollowup, interviewEnded));
        return result;
    }

    private Map<String, Object> buildChatResponseData(String sessionId, InterviewSession session,
            String aiResponse, boolean isFollowup, boolean interviewEnded) {
        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", sessionId);
        data.put("reply", aiResponse);
        data.put("questionCount", session.getQuestionCount());
        data.put("maxQuestions", session.getMaxQuestions());
        data.put("followupCount", session.getFollowupCount());
        data.put("maxFollowups", session.getMaxFollowups());
        data.put("isFollowup", isFollowup);
        data.put("interviewEnded", interviewEnded);
        return data;
    }

    private Map<String, Object> handleEmptyMessage(InterviewSession session, String sessionId) throws Exception {
        System.out.println("[DEBUG] handleEmptyMessage - interviewType: " + session.getInterviewType());
        String aiResponse = aiService.generateNewQuestionResponse(
            session.getResumeText(),
            session.getPosition(),
            session.getTempMessages(),
            session.getUserId(),
            session.getInterviewType()
        );
        session.setQuestionCount(session.getQuestionCount() + 1);
        session.setLastWasFollowup(false);
        session.setFollowupCount(0);
        session.getTempMessages().add(new MessageContext("assistant", aiResponse));
        return buildChatResponse(sessionId, session, aiResponse, false, false);
    }

    private boolean shouldAskFollowup(InterviewSession session) {
        return session.getQuestionCount() > 0 && session.getFollowupCount() < session.getMaxFollowups();
    }

    private boolean shouldEndInterview(InterviewSession session) {
        return session.getQuestionCount() >= session.getMaxQuestions();
    }

    private Map<String, Object> generateEndResponse(InterviewSession session, String sessionId) {
        session.setStatus("COMPLETED");
        sessionRepository.save(session);
        Long dbSessionId = session.getId();
        saveTextMessages(session);

        if (!abilityEvaluationRepository.findBySessionId(session.getId()).isPresent()) {
            asyncEvaluationService.performEvaluationAsync(
                session.getId(),
                session.getSessionType(),
                session.getResumeText(),
                session.getPosition(),
                session.getUserId()
            );
        }

        activeSessions.remove(sessionId);

        String endMessage = "面试已结束，您已完成所有问题。";
        Map<String, Object> responseData = buildChatResponseData(sessionId, session, endMessage, false, true);
        responseData.put("dbSessionId", dbSessionId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", responseData);
        return result;
    }

    private Map<String, Object> buildVoiceResponse(String sessionId, String reply, String transcript,
            Map<String, Object> audioFeatures, Double duration, int questionCount, int maxQuestions,
            int followupCount, int maxFollowups, boolean isFollowup, boolean interviewEnded) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);

        String finalReply = reply;
        if (interviewEnded) {
            finalReply += "\n\n【面试已结束】感谢您的参与，您已经回答了" + questionCount + "个主要问题。";
        }

        result.put("data", buildVoiceResponseData(sessionId, finalReply, transcript, audioFeatures, duration,
            questionCount, maxQuestions, followupCount, maxFollowups, isFollowup, interviewEnded));
        return result;
    }

    private Map<String, Object> buildVoiceResponseData(String sessionId, String reply, String transcript,
            Map<String, Object> audioFeatures, Double duration, int questionCount, int maxQuestions,
            int followupCount, int maxFollowups, boolean isFollowup, boolean interviewEnded) {
        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", sessionId);
        data.put("reply", reply);
        data.put("transcript", transcript);
        data.put("audioFeatures", audioFeatures);
        data.put("duration", duration);
        data.put("questionCount", questionCount);
        data.put("maxQuestions", maxQuestions);
        data.put("followupCount", followupCount);
        data.put("maxFollowups", maxFollowups);
        data.put("isFollowup", isFollowup);
        data.put("interviewEnded", interviewEnded);
        return data;
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 60000)
    public void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        int removedCount = 0;
        var iterator = activeSessions.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (now - entry.getValue().getLastActiveTime() > SESSION_TIMEOUT_MS) {
                iterator.remove();
                removedCount++;
            }
        }
        if (removedCount > 0) {
            System.out.println("[Session Cleanup] Removed " + removedCount + " expired sessions");
        }
    }
}