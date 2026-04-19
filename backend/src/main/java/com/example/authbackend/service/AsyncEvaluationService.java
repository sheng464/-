package com.example.authbackend.service;

import com.example.authbackend.entity.AbilityEvaluation;
import com.example.authbackend.entity.InterviewSession;
import com.example.authbackend.entity.TextInterviewMessage;
import com.example.authbackend.entity.VoiceInterviewMessage;
import com.example.authbackend.repository.AbilityEvaluationRepository;
import com.example.authbackend.repository.TextInterviewMessageRepository;
import com.example.authbackend.repository.VoiceInterviewMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AsyncEvaluationService {

    @Autowired
    private AbilityEvaluationRepository abilityEvaluationRepository;

    @Autowired
    private TextInterviewMessageRepository textMessageRepository;

    @Autowired
    private VoiceInterviewMessageRepository voiceMessageRepository;

    @Autowired
    private AIService aiService;

    @Autowired
    private ExcellentAnswerService excellentAnswerService;

    public static final String SESSION_TYPE_TEXT = "TEXT";
    public static final String SESSION_TYPE_VOICE = "VOICE";

    @Async
    public void performEvaluationAsync(Long sessionId, String sessionType, String resumeText, String position, Long userId) {
        try {
            if (abilityEvaluationRepository.findBySessionId(sessionId).isPresent()) {
                return;
            }

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

            String evaluationPrompt = buildEvaluationPrompt(sessionType, resumeText, position, conversation.toString());
            String evaluationResponse = aiService.generateAbilityEvaluation(evaluationPrompt);

            AbilityEvaluation evaluation = parseEvaluationResult(sessionId, sessionType, userId, position, evaluationResponse);
            abilityEvaluationRepository.save(evaluation);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String buildEvaluationPrompt(String sessionType, String resumeText, String position, String conversation) {
        String structuredQA = structureConversationAsQAPairs(conversation);

        String questionsFromConversation = extractQuestionsFromConversation(conversation);
        String excellentAnswersContext = "";
        try {
            excellentAnswersContext = excellentAnswerService.retrieveRelevantExcellentAnswers(
                questionsFromConversation, 3
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

        String fullPrompt;
        if (SESSION_TYPE_TEXT.equals(sessionType)) {
            fullPrompt = String.format("""
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
                5. 如果回答与问题无关、答非所问、乱说一通或明显错误，评分应该很低（0-30分）

                完整面试记录：
                %s

                请对面试者的回答进行深度能力分析，评估以下方面：
                1. 技术正确性：回答中的技术知识是否准确
                2. 知识深度：对技术问题的理解深度和广度
                3. 逻辑严谨性：回答的逻辑是否清晰、严谨

                【评分标准】
                - 优秀(80-100)：回答准确、深入、有逻辑
                - 良好(60-79)：回答基本正确，但深度不足
                - 一般(40-59)：回答有错误或深度较差
                - 较差(20-39)：回答有较多错误或答非所问
                - 很差(0-19)：回答与问题无关、乱回答、明显错误

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
                """, position, resumeText, excellentPrompt, structuredQA);
        } else {
            fullPrompt = String.format("""
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
                5. 如果回答与问题无关、答非所问、乱说一通或明显错误，评分应该很低（0-30分）

                完整面试记录：
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

                【评分标准】
                - 优秀(80-100)：回答准确、深入、有逻辑
                - 良好(60-79)：回答基本正确，但深度不足
                - 一般(40-59)：回答有错误或深度较差
                - 较差(20-39)：回答有较多错误或答非所问
                - 很差(0-19)：回答与问题无关、乱回答、明显错误

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
                """, position, resumeText, excellentPrompt, structuredQA);
        }

        System.out.printf("===== ASYNC EVALUATION PROMPT START =====\n%s\n===== ASYNC EVALUATION PROMPT END =====\n", fullPrompt);
        return fullPrompt;
    }

    private String extractQuestionsFromConversation(String conversation) {
        StringBuilder questions = new StringBuilder();
        String[] lines = conversation.split("\n");
        for (String line : lines) {
            if (line.startsWith("面试官：")) {
                if (questions.length() > 0) {
                    questions.append(" ");
                }
                questions.append(line.substring(4));
            }
        }
        return questions.toString();
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

    private AbilityEvaluation parseEvaluationResult(Long sessionId, String sessionType, Long userId, String position, String evaluationResponse) {
        AbilityEvaluation evaluation = new AbilityEvaluation();
        evaluation.setSessionId(sessionId);
        evaluation.setSessionType(sessionType);
        evaluation.setUserId(userId);
        evaluation.setPosition(position);
        evaluation.setFullAnalysis(evaluationResponse);

        try {
            String jsonStr = extractJsonFromText(evaluationResponse);
            if (jsonStr != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>> typeRef =
                    new com.fasterxml.jackson.core.type.TypeReference<>() {};
                java.util.Map<String, Object> json = mapper.readValue(jsonStr, typeRef);

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
        if (value instanceof java.util.Map) {
            java.util.Map<?, ?> map = (java.util.Map<?, ?>) value;
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for (java.util.Map.Entry<?, ?> entry : map.entrySet()) {
                if (i > 0) sb.append("\n");
                sb.append("- ").append(entry.getKey()).append(": ").append(entry.getValue());
                i++;
            }
            return sb.toString();
        }
        return value.toString();
    }
}