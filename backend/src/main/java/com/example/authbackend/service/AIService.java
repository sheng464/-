package com.example.authbackend.service;

import com.example.authbackend.dto.MessageContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class AIService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.ai.ollama.chat.options.model}")
    private String model;

    private static final int MAX_CONTENT_LENGTH = 8000;

    @Autowired
    private RagService ragService;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    private static final Map<String, String> POSITION_TYPE_MAP = Map.of(
        "java_backend", "Java后端",
        "web_frontend", "Web前端"
    );

    private static final Map<String, String> KNOWLEDGE_QUERY_MAP = Map.of(
        "java_backend", "Java后端 框架 数据库 微服务",
        "web_frontend", "Web前端 Vue React CSS HTML"
    );

    public AIService(RestClient.Builder restClientBuilder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(180000);
        this.restClient = restClientBuilder
                .baseUrl("http://localhost:11434")
                .requestFactory(factory)
                .build();
    }

    public Map<String, Object> parseResume(MultipartFile file) throws Exception {
        String content = extractText(file);
        System.out.println("[DEBUG] Extracted content length: " + content.length());
        String prompt = buildPrompt(content);
        String aiResponse = requestAI(prompt);
        System.out.println("[DEBUG] AI Response: " + aiResponse);
        Map<String, Object> result = parseResponse(aiResponse);
        System.out.println("[DEBUG] Parsed result: " + result);
        return result;
    }

    public String extractText(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();

        if (fileName == null) {
            return "";
        }

        String lowerName = fileName.toLowerCase();

        if (lowerName.endsWith(".pdf")) {
            return extractPdf(file);
        } else if (lowerName.endsWith(".docx") || lowerName.endsWith(".doc")) {
            return extractDocx(file);
        }

        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }

    private String extractPdf(MultipartFile file) throws Exception {
        try (PDDocument doc = PDDocument.load(file.getBytes())) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private String extractDocx(MultipartFile file) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(file.getBytes()))) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph para : doc.getParagraphs()) {
                sb.append(para.getText()).append("\n");
            }
            return sb.toString();
        }
    }

    public String extractTextFromInputStream(InputStream inputStream, String fileName) throws Exception {
        if (fileName == null) {
            return "";
        }
        String lowerName = fileName.toLowerCase();
        byte[] bytes = inputStream.readAllBytes();

        if (lowerName.endsWith(".pdf")) {
            try (PDDocument doc = PDDocument.load(bytes)) {
                return new PDFTextStripper().getText(doc);
            }
        } else if (lowerName.endsWith(".docx") || lowerName.endsWith(".doc")) {
            try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
                StringBuilder sb = new StringBuilder();
                for (XWPFParagraph para : doc.getParagraphs()) {
                    sb.append(para.getText()).append("\n");
                }
                return sb.toString();
            }
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public String checkPositionMatch(String resumeText, String position) throws Exception {
        String prompt = String.format("""
            你是一个严格的HR面试官。请严格判断简历内容是否与目标岗位匹配。

            【匹配定义】
            - MATCH（匹配）：简历中的技术栈、工作经历与目标岗位直接相关（如：Java后端简历 vs Java后端岗位）
            - MISMATCH（不匹配）：简历与目标岗位毫不相关（如：Java后端简历 vs web前端岗位）

            【重要原则】
            1. 严格判断，禁止宽松匹配
            2. 只要简历中的主要技术栈或工作方向与目标岗位不符，即为MISMATCH
            3. 同属计算机类专业不算匹配，必须技术栈直接相关才算
            4. 示例：
               - Java后端简历 vs Java后端岗位 → MATCH
               - Java后端简历 vs web前端岗位 → MISMATCH
               - Java后端简历 vs Python后端岗位 → PARTIAL（部分技术重叠）
               - Java后端简历 vs 测试工程师岗位 → MISMATCH

            简历内容：
            %s

            目标岗位：%s

            请返回一个JSON对象，包含以下字段：
            - match: "MATCH"(匹配), "PARTIAL"(部分匹配), "MISMATCH"(不匹配)
            - reason: 判断理由

            只返回JSON，不要其他内容。
            """, resumeText, position);

        String response = requestAI(prompt);
        try {
            String json = cleanJsonResponse(response);
            JsonNode node = objectMapper.readTree(json);
            System.out.println(node);
            String match = node.get("match").asText();
            return match;
        } catch (Exception e) {
            return "MISMATCH";
        }
    }

    public boolean shouldAskFollowup(List<MessageContext> messages) throws Exception {
        if (messages.size() < 2) {
            return false;
        }

        String lastQuestion = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            MessageContext msg = messages.get(i);
            if ("assistant".equals(msg.getRole())) {
                lastQuestion = msg.getContent();
                break;
            }
        }

        if (lastQuestion.isEmpty()) {
            return false;
        }

        String lastAnswer = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            MessageContext msg = messages.get(i);
            if ("user".equals(msg.getRole())) {
                lastAnswer = msg.getContent();
                break;
            }
        }

        if (lastAnswer.isEmpty()) {
            return false;
        }

        String prompt = String.format("""
            判断以下面试追问必要性：

            面试官追问：%s

            面试者回答：%s

            判断规则：
            - 如果面试者回答较浅、不完整或值得深入挖掘 → 回答"是"
            - 如果面试者回答已经很完整深入 → 回答"否"

            只回答"是"或"否"，不要其他内容。
            """, lastQuestion, lastAnswer);

        String response = requestAI(prompt).trim().toLowerCase();
        return response.startsWith("是");
    }

    public String generateFollowupResponse(
            String resumeText,
            String position,
            List<MessageContext> messages,
            int followupCount,
            int maxFollowups,
            Long userId,
            Long resumeId) throws Exception {

        String lastMessage = messages.isEmpty() ? "" : messages.get(messages.size() - 1).getContent();
        String lastQuestion = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            MessageContext msg = messages.get(i);
            if ("user".equals(msg.getRole())) {
                lastQuestion = msg.getContent();
                break;
            }
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            MessageContext msg = messages.get(i);
            if ("assistant".equals(msg.getRole())) {
                lastQuestion = msg.getContent();
                break;
            }
        }

        String normalizedPosition = normalizePosition(position);
        String knowledgeContext = "";
        try {
            String knowledgeQuery = lastQuestion + " " + lastMessage;
            knowledgeContext = knowledgeBaseService.retrieveFromKnowledgeBase(knowledgeQuery, 3);
        } catch (Exception e) {
            System.err.println("Failed to retrieve knowledge base for followup: " + e.getMessage());
        }

        String prompt = buildFollowupPromptContent(resumeText, position, lastQuestion, lastMessage, knowledgeContext);
        return requestAI(prompt);
    }

    public String buildFollowupPrompt(
            String resumeText,
            String position,
            List<MessageContext> messages,
            int followupCount,
            int maxFollowups,
            Long userId,
            Long resumeId) throws Exception {

        String lastMessage = messages.isEmpty() ? "" : messages.get(messages.size() - 1).getContent();
        String lastQuestion = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            MessageContext msg = messages.get(i);
            if ("user".equals(msg.getRole())) {
                lastQuestion = msg.getContent();
                break;
            }
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            MessageContext msg = messages.get(i);
            if ("assistant".equals(msg.getRole())) {
                lastQuestion = msg.getContent();
                break;
            }
        }

        String normalizedPosition = normalizePosition(position);
        String knowledgeContext = "";
        try {
            String knowledgeQuery = lastQuestion + " " + lastMessage;
            knowledgeContext = knowledgeBaseService.retrieveFromKnowledgeBase(knowledgeQuery, 3);
        } catch (Exception e) {
            System.err.println("Failed to retrieve knowledge base for followup: " + e.getMessage());
        }

        String prompt = buildFollowupPromptContent(resumeText, position, lastQuestion, lastMessage, knowledgeContext);
        System.out.println("===== PROMPT START =====");
        System.out.println(prompt);
        System.out.println("===== PROMPT END =====");
        return prompt;
    }

    private String buildFollowupPromptContent(String resumeText, String position, String lastQuestion, String lastAnswer, String knowledgeContext) {
        String positionType = POSITION_TYPE_MAP.getOrDefault(normalizePosition(position), position);
        String normalizedResume = resumeText.length() > 2000
            ? resumeText.substring(0, 2000) + "..."
            : resumeText;

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("面试背景：\n");
        promptBuilder.append("目标岗位：").append(positionType).append("\n\n");
        promptBuilder.append("简历内容：\n").append(normalizedResume.trim()).append("\n\n");

        if (knowledgeContext != null && !knowledgeContext.isEmpty()) {
            promptBuilder.append("【相关知识库内容】\n").append(knowledgeContext).append("\n\n");
        }

        promptBuilder.append(String.format("""
            面试官追问：%s

            面试者回答：%s

            作为一位专业的AI面试官，请在之前的回答基础上参考相关知识库内容和追问指令深入追问。
            要求：
            1. 追问要有深度，挖掘更多信息
            2. 不要问新话题
            3. 语言简洁专业，像真实面试官一样

            直接输出追问问题，不要有多余的解释。
            """, lastQuestion, lastAnswer));

        return promptBuilder.toString();
    }

    public String buildNewQuestionPrompt(
            String resumeText,
            String position,
            List<MessageContext> messages,
            Long userId,
            String interviewType) throws Exception {

        boolean isFirstQuestion = messages.isEmpty() || (messages.size() == 1 && messages.get(messages.size() - 1).getContent().isEmpty());

        String prompt;
        if ("TECH".equals(interviewType)) {
            String lastMessage = messages.isEmpty() ? "" : messages.get(messages.size() - 1).getContent();
            String techSkills = "";
            String knowledgeContext = "";
            try {
                techSkills = extractTechSkillsFromResume(resumeText, position);
            } catch (Exception e) {
                System.err.println("Failed to extract tech skills for TECH interview: " + e.getMessage());
            }
            try {
                String knowledgeQuery = techSkills;
                knowledgeContext = knowledgeBaseService.retrieveFromKnowledgeBase(knowledgeQuery, 2);
            } catch (Exception e) {
                System.err.println("Failed to retrieve knowledge base for TECH interview: " + e.getMessage());
            }
            prompt = buildTechQuestionPrompt(position, techSkills, knowledgeContext, lastMessage, isFirstQuestion, messages);
        } else {
            String conversationContext = buildConversationContext(resumeText, position, messages, userId);
            prompt = buildProjectQuestionPrompt(resumeText, conversationContext, isFirstQuestion);
        }

        System.out.println("===== PROMPT START =====");
        System.out.println(prompt);
        System.out.println("===== PROMPT END =====");
        return prompt;
    }

    private String buildProjectQuestionPrompt(String resumeText, String conversationContext, boolean isFirstQuestion) {
        if (isFirstQuestion) {
            return String.format("""
                %s

                作为一位专业的AI面试官，请根据简历、岗位和相关知识库内容生成第一个面试问题。
                要求：
                1. 必须是开放式问题，不能是简单的是非题
                2. 要基于简历中的经历来提问
                3. 不要询问简历中已有的基本信息
                4. 语言简洁专业，像真实面试官一样

                直接输出问题，不要有多余的解释。
                """, conversationContext);
        } else {
            String lastMessage = conversationContext.isEmpty() ? "" : conversationContext;
            return String.format("""
                %s

                面试者刚才回答说：%s

                作为一位专业的AI面试官，请问一个与简历和专业相关的新面试问题。
                要求：
                1. 不要重复问同样的问题
                2. 不要在原话题上继续追问
                3. 语言简洁专业，像真实面试官一样

                直接输出面试官的问题，不要有多余的解释。
                """, conversationContext, lastMessage);
        }
    }

    private String buildTechQuestionPrompt(String position, String resumeContext, String knowledgeContext, String lastAnswer, boolean isFirstQuestion, List<MessageContext> messages) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("你是一位专业的AI技术面试官，面试岗位是：").append(position).append("\n\n");

        if (resumeContext != null && !resumeContext.isEmpty()) {
            promptBuilder.append("候选人简历中提到的技术背景：\n").append(resumeContext).append("\n\n");
        }

        if (knowledgeContext != null && !knowledgeContext.isEmpty()) {
            promptBuilder.append("相关知识库内容：\n").append(knowledgeContext).append("\n\n");
        }

        if (!messages.isEmpty()) {
            promptBuilder.append("面试对话历史：\n");
            for (MessageContext msg : messages) {
                String role = msg.getRole().equals("user") ? "面试者" : "面试官";
                promptBuilder.append(role).append("：").append(msg.getContent()).append("\n");
            }
            promptBuilder.append("\n");
        }

        if (isFirstQuestion) {
            promptBuilder.append(String.format("""
                请生成一道技术基础知识面试题。
                要求：
                1. 必须是基础概念题，答案简单确定（如：是什么？有什么用？有哪几种？）
                2. 禁止出场景题、方案设计题、举例说明题
                3. 基于候选人简历中的技术栈，面试岗位和知识库内容
                4. 语言简洁，像真实面试官

                直接输出问题，不要有多余的解释。
                """));
        } else {
            promptBuilder.append(String.format("""
                面试者刚才回答说：%s

                请生成一道新的技术基础知识面试题。
                要求：
                1. 必须是基础概念题，答案简单确定（如：是什么？有什么用？有哪几种？）
                2. 禁止出场景题、方案设计题、举例说明题
                3. 不要重复问同样的问题
                4. 基于候选人简历中的技术栈，面试岗位和知识库内容
                5. 语言简洁，像真实面试官

                直接输出面试官的问题，不要有多余的解释。
                """, lastAnswer));
        }

        return promptBuilder.toString();
    }

    public String extractTechSkillsFromResume(String resumeText, String position) {
        String prompt = String.format("""
            从以下简历内容中提取技术掌握点和技能关键词。
            
            简历内容：
            %s
            
            目标岗位：%s
            
            请提取候选人掌握的技术栈、框架、工具、数据库等技能关键词，用逗号分隔返回。
            只返回技能关键词列表，不要其他解释。
            格式示例：Java, Spring Boot, MySQL, Redis, Git, Docker
            """, resumeText, position);

        try {
            return requestAI(prompt).trim();
        } catch (Exception e) {
            System.err.println("Failed to extract tech skills: " + e.getMessage());
            return "";
        }
    }

    private String getTechTopicPool(String normalizedPosition) {
        return switch (normalizedPosition) {
            case "java_backend" -> """
                Java基础：多线程、集合、并发、JVM、反射、泛型
                Spring：IOC、AOP、事务、SpringBoot自动配置、源码理解
                数据库：索引原理、事务隔离级别、SQL优化、锁机制、分库分表
                中间件：Redis缓存、消息队列、分布式事务
                设计模式：单例、工厂、代理、策略、观察者
                """;
            case "web_frontend" -> """
                JavaScript基础：原型链、作用域、闭包、异步、Event Loop
                Vue/React：响应式原理、虚拟DOM、生命周期、组件通信
                CSS：盒模型、Flex布局、Grid布局、定位、BFC
                网络：HTTP/HTTPS、TCP三次握手、WebSocket、CORS
                工程化：Webpack配置、ESLint、Git工作流
                """;
            default -> """
                编程基础、数据结构与算法、计算机网络、
                数据库、操作系统、主流框架原理
                """;
        };
    }

    public String generateNewQuestionResponse(
            String resumeText,
            String position,
            List<MessageContext> messages,
            Long userId,
            String interviewType) throws Exception {

        boolean isFirstQuestion = messages.isEmpty() || (messages.size() == 1 && messages.get(messages.size() - 1).getContent().isEmpty());

        String prompt;
        String knowledgeContext = "";

        if ("TECH".equals(interviewType)) {
            String lastAnswer = messages.isEmpty() ? "" : messages.get(messages.size() - 1).getContent();
            String techSkills = "";
            try {
                techSkills = extractTechSkillsFromResume(resumeText, position);
            } catch (Exception e) {
                System.err.println("Failed to extract tech skills for TECH interview: " + e.getMessage());
            }
            try {
                String knowledgeQuery = techSkills;
                knowledgeContext = knowledgeBaseService.retrieveFromKnowledgeBase(knowledgeQuery, 2);
            } catch (Exception e) {
                System.err.println("Failed to retrieve knowledge base for TECH interview: " + e.getMessage());
            }
            prompt = buildTechQuestionPrompt(position, techSkills, knowledgeContext, lastAnswer, isFirstQuestion, messages);
        } else {
            String conversationContext = buildConversationContext(resumeText, position, messages, userId);

            String techSkills = "";
            try {
                techSkills = extractTechSkillsFromResume(resumeText, position);
            } catch (Exception e) {
                System.err.println("Failed to extract tech skills for PROJECT interview: " + e.getMessage());
            }
            try {
                String knowledgeQuery = techSkills.isEmpty() ? position : techSkills;
                knowledgeContext = knowledgeBaseService.retrieveFromKnowledgeBase(knowledgeQuery, 3);
            } catch (Exception e) {
                System.err.println("Failed to retrieve knowledge base for new question: " + e.getMessage());
            }

            prompt = buildProjectQuestionPromptForGenerate(resumeText, conversationContext, isFirstQuestion, messages, knowledgeContext);
        }

        System.out.println("===== PROMPT START =====");
        System.out.println(prompt);
        System.out.println("===== PROMPT END =====");
        return requestAI(prompt);
    }

    private String buildProjectQuestionPromptForGenerate(String resumeText, String conversationContext, boolean isFirstQuestion, List<MessageContext> messages, String knowledgeContext) {
        if (isFirstQuestion) {
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append(conversationContext);
            if (knowledgeContext != null && !knowledgeContext.isEmpty()) {
                promptBuilder.append("【相关知识库内容】\n").append(knowledgeContext).append("\n\n");
            }
            promptBuilder.append("""
                作为一位专业的AI面试官，请根据简历、岗位和相关知识库内容生成第一个面试问题。
                要求：
                1. 必须是开放式问题，不能是简单的是非题
                2. 要基于简历中的经历来提问
                3. 不要询问简历中已有的基本信息
                4. 语言简洁专业，像真实面试官一样

                直接输出问题，不要有多余的解释。
                """);
            return promptBuilder.toString();
        } else {
            String lastMessage = messages.isEmpty() ? "" : messages.get(messages.size() - 1).getContent();
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append(conversationContext);
            if (knowledgeContext != null && !knowledgeContext.isEmpty()) {
                promptBuilder.append("【相关知识库内容】\n").append(knowledgeContext).append("\n\n");
            }
            promptBuilder.append(String.format("""
                面试者刚才回答说：%s

                作为一位专业的AI面试官，请问一个与简历和专业相关的新面试问题。
                要求：
                1. 不要重复问同样的问题
                2. 不要在原话题上继续追问
                3. 语言简洁专业，像真实面试官一样

                直接输出面试官的问题，不要有多余的解释。
                """, lastMessage));
            return promptBuilder.toString();
        }
    }

    private String buildConversationContext(String resumeText, String position, List<MessageContext> messages, Long userId) throws Exception {
        String lastMessage = messages.isEmpty() ? "" : messages.get(messages.size() - 1).getContent();
        boolean isFirstQuestion = messages.isEmpty() || (messages.size() == 1 && lastMessage.isEmpty());

        String normalizedPosition = normalizePosition(position);
        String positionType = POSITION_TYPE_MAP.getOrDefault(normalizedPosition, position);

        String normalizedResume = resumeText.length() > 2000
            ? resumeText.substring(0, 2000) + "..."
            : resumeText;

        StringBuilder conversation = new StringBuilder();
        conversation.append("面试背景：\n");
        conversation.append("目标岗位：").append(positionType).append("\n\n");
        conversation.append("简历内容：\n").append(normalizedResume.trim()).append("\n\n");

        String knowledgeContext = "";
        try {
            String techSkills = extractTechSkillsFromResume(resumeText, position);
            String knowledgeQuery = techSkills.isEmpty() ? KNOWLEDGE_QUERY_MAP.getOrDefault(normalizedPosition, "Java 后端 前端") : techSkills;
            knowledgeContext = knowledgeBaseService.retrieveFromKnowledgeBase(knowledgeQuery, 3);
        } catch (Exception e) {
            System.err.println("Failed to retrieve from knowledge base: " + e.getMessage());
        }

        if (!knowledgeContext.isEmpty()) {
            conversation.append("【相关知识库内容】\n").append(knowledgeContext.trim()).append("\n\n");
        }

        StringBuilder dialogHistory = new StringBuilder();
        for (MessageContext msg : messages) {
            String role = msg.getRole().equals("user") ? "面试者" : "面试官";
            dialogHistory.append(role).append("：").append(msg.getContent()).append("\n");
        }
        conversation.append("面试对话：\n");
        conversation.append(dialogHistory);

        return conversation.toString();
    }

    public String generateVoiceEvaluation(
            String resumeText,
            String position,
            String transcript,
            Map<String, Object> audioFeatures,
            Long userId) throws Exception {

        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的面试评估助手。请根据以下数据对候选人进行综合评估：\n\n");

        prompt.append("【简历摘要】\n");
        String normalizedResume = resumeText != null && resumeText.length() > 500
            ? resumeText.substring(0, 500)
            : (resumeText != null ? resumeText : "");
        prompt.append(normalizedResume);
        prompt.append("\n\n");

        prompt.append("【目标岗位】\n");
        prompt.append(position).append("\n\n");

        String normalizedPosition = normalizePosition(position);
        String excellentExamples = "";
        try {
            String query = "优秀面试回答 评分标准 " + normalizedPosition;
            excellentExamples = ragService.buildContextFromDocuments(
                ragService.retrieveRelevantContext(query, 3, userId));
        } catch (Exception e) {
            System.err.println("Failed to retrieve excellent examples: " + e.getMessage());
        }
        if (!excellentExamples.isEmpty()) {
            prompt.append("【优秀问答参考】\n").append(excellentExamples).append("\n\n");
        }

        prompt.append("【语音特征】\n");
        if (audioFeatures != null) {
            prompt.append("- 语速：").append(audioFeatures.getOrDefault("speechRate", 0)).append(" 字/分钟\n");
            prompt.append("- 停顿次数：").append(audioFeatures.getOrDefault("pauseCount", 0)).append(" 次\n");
            prompt.append("- 音量稳定性：").append(audioFeatures.getOrDefault("energyStability", 0)).append(" (0-1)\n");
            prompt.append("- 语调变化：").append(audioFeatures.getOrDefault("pitchVariation", 0)).append(" (0-1)\n");
            prompt.append("- filler words：").append(audioFeatures.getOrDefault("fillerWordCount", 0)).append(" 次\n");
            prompt.append("- 情绪倾向：").append(audioFeatures.getOrDefault("emotionTendency", "unknown")).append("\n");
            prompt.append("- 回答时长：").append(audioFeatures.getOrDefault("duration", 0)).append(" 秒\n");
        } else {
            prompt.append("[无语音数据]\n");
        }
        prompt.append("\n");

        prompt.append("【回答内容】\n");
        prompt.append(transcript != null ? transcript : "[无内容]");
        prompt.append("\n\n");

        prompt.append("请给出简洁的评价，包括：\n");
        prompt.append("1. 表达流畅度（基于语音特征）\n");
        prompt.append("2. 回答质量（基于内容）\n");
        prompt.append("3. 综合评分（1-10分）\n");

        return requestAI(prompt.toString());
    }

    public String generateAbilityEvaluation(String prompt) {
        return requestAI(prompt);
    }

    private String buildPrompt(String content) {
        String truncated = content.length() > MAX_CONTENT_LENGTH
            ? content.substring(0, MAX_CONTENT_LENGTH)
            : content;

        return """
            你是一个专业的简历解析助手。请从以下简历内容中提取关键信息，并以JSON格式返回。

            要求：
            1. 只返回JSON，不要其他内容
            2. 字段：name(姓名), gender(性别), age(年龄数字), education(学历), phone(手机号), email(邮箱), position(期望岗位), skills(技能标签逗号分隔), experience(工作经历摘要)
            3. 不存在的字段返回空字符串""

            简历内容：%s

            返回JSON：""".formatted(truncated);
    }

    private String requestAI(String prompt) {
        Map<String, Object> body = Map.of(
            "model", model,
            "prompt", prompt,
            "stream", false
        );

        System.out.println("[DEBUG] Requesting AI with model: " + model);
        String response = restClient.post()
                .uri("/api/generate")
                .body(body)
                .retrieve()
                .body(String.class);

        try {
            JsonNode node = objectMapper.readTree(response);
            return node.get("response").asText();
        } catch (Exception e) {
            System.err.println("[DEBUG] Failed to parse AI response: " + e.getMessage());
            throw new RuntimeException("解析AI响应失败: " + e.getMessage());
        }
    }

    public void requestAIStream(String prompt, StreamCallback callback) {
        new Thread(() -> {
            try {
                Map<String, Object> body = Map.of(
                    "model", model,
                    "prompt", prompt,
                    "stream", true
                );

                System.out.println("[DEBUG] Requesting AI stream with model: " + model);

                URL url = new URL("http://localhost:11434/api/generate");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(180000);

                String jsonBody = objectMapper.writeValueAsString(body);
                connection.getOutputStream().write(jsonBody.getBytes(StandardCharsets.UTF_8));
                connection.getOutputStream().flush();

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    callback.onError(new RuntimeException("Ollama API error: " + responseCode));
                    connection.disconnect();
                    return;
                }

                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
                );

                String line;
                while ((line = reader.readLine()) != null) {
                    String extracted = extractStreamResponse(line);
                    if (!extracted.isEmpty()) {
                        if ("[DONE]".equals(extracted)) {
                            break;
                        }
                        callback.onChunk(extracted);
                    }
                }
                reader.close();
                connection.disconnect();
                callback.onComplete();

            } catch (Exception e) {
                System.err.println("[DEBUG] Stream error: " + e.getMessage());
                callback.onError(e);
            }
        }).start();
    }

    public interface StreamCallback {
        void onChunk(String chunk);
        void onComplete();
        void onError(Exception e);
    }

    private String extractStreamResponse(String line) {
        try {
            line = line.trim();
            if (line.isEmpty()) {
                return "";
            }
            JsonNode node = objectMapper.readTree(line);
            if (node.has("response")) {
                return node.get("response").asText();
            }
            if (node.has("done") && node.get("done").asBoolean()) {
                return "[DONE]";
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    private Map<String, Object> parseResponse(String response) {
        Map<String, Object> result = new HashMap<>();

        try {
            String json = cleanJsonResponse(response);
            System.out.println("[DEBUG] Cleaned JSON: " + json);
            JsonNode node = objectMapper.readTree(json);

            result.put("name", getText(node, "name"));
            result.put("gender", getText(node, "gender"));
            result.put("age", getInt(node, "age"));
            result.put("education", getText(node, "education"));
            result.put("phone", getText(node, "phone"));
            result.put("email", getText(node, "email"));
            result.put("position", getText(node, "position"));
            result.put("skills", getText(node, "skills"));
            result.put("experience", getText(node, "experience"));

        } catch (Exception e) {
            System.err.println("[DEBUG] Parse error: " + e.getMessage());
            return getEmptyResult();
        }

        return result;
    }

    private String cleanJsonResponse(String response) {
        return response.trim()
                .replaceAll("^```json", "")
                .replaceAll("^```", "")
                .replaceAll("```$", "")
                .trim();
    }

    private String getText(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asText("") : "";
    }

    private Integer getInt(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asInt();
    }

    private Map<String, Object> getEmptyResult() {
        Map<String, Object> empty = new HashMap<>();
        empty.put("name", "");
        empty.put("gender", "");
        empty.put("age", null);
        empty.put("education", "");
        empty.put("phone", "");
        empty.put("email", "");
        empty.put("position", "");
        empty.put("skills", "");
        empty.put("experience", "");
        return empty;
    }

    private String normalizePosition(String position) {
        if (position == null || position.isEmpty()) {
            return "java_backend";
        }
        String lower = position.toLowerCase();
        if (lower.contains("java") && lower.contains("后端")) {
            return "java_backend";
        }
        if (lower.contains("web") || lower.contains("前端") || lower.contains("vue") || lower.contains("react")) {
            return "web_frontend";
        }
        if (lower.contains("java")) {
            return "java_backend";
        }
        return position;
    }

}
