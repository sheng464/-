package com.example.authbackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.HashMap;
import java.util.Map;

@Service
public class AudioFeatureService {

    private final RestClient restClient;

    @Value("${spring.ai.whisper.url:http://localhost:5000}")
    private String whisperUrl;

    public AudioFeatureService(RestClient.Builder restClientBuilder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(60000);
        factory.setReadTimeout(120000);
        this.restClient = restClientBuilder
                .baseUrl("http://localhost:5000")
                .requestFactory(factory)
                .build();
    }

    public Map<String, Object> processAudio(byte[] audioData) throws Exception {
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> whisperResponse = callWhisperAPI(audioData);

        String transcript = (String) whisperResponse.getOrDefault("text", "");
        @SuppressWarnings("unchecked")
        Map<String, Object> audioFeatures = (Map<String, Object>) whisperResponse.getOrDefault("audio_features", new HashMap<>());

        result.put("transcript", transcript);
        result.put("audioFeatures", audioFeatures);

        if (audioFeatures.containsKey("duration")) {
            result.put("duration", audioFeatures.get("duration"));
        } else {
            result.put("duration", 0);
        }

        return result;
    }

    private Map<String, Object> callWhisperAPI(byte[] audioData) throws Exception {
        try {
            System.out.println("[Whisper] Calling Whisper API with audio data size: " + (audioData != null ? audioData.length : "null"));
            String response = restClient.post()
                    .uri("/v1/audio/transcriptions")
                    .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                    .body(audioData)
                    .retrieve()
                    .body(String.class);
            System.out.println("[Whisper] Response: " + response);

            return parseWhisperResponse(response);
        } catch (Exception e) {
            System.err.println("[Whisper] Error calling Whisper API: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("text", "[语音识别失败，请稍后重试]");
            Map<String, Object> emptyFeatures = createEmptyFeatures();
            errorResult.put("audio_features", emptyFeatures);
            return errorResult;
        }
    }

    private Map<String, Object> createEmptyFeatures() {
        Map<String, Object> features = new HashMap<>();
        features.put("duration", 0);
        features.put("speechRate", 0);
        features.put("pauseCount", 0);
        features.put("pauseTotalSeconds", 0);
        features.put("energyStability", 0);
        features.put("pitchVariation", 0);
        features.put("fillerWordCount", 0);
        features.put("emotionTendency", "unknown");
        return features;
    }

    private Map<String, Object> parseWhisperResponse(String response) {
        Map<String, Object> result = new HashMap<>();

        if (response == null || response.isEmpty()) {
            result.put("text", "");
            result.put("audio_features", createEmptyFeatures());
            return result;
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(response);

            String text = node.has("text") ? node.get("text").asText().trim() : "";
            result.put("text", text);

            Map<String, Object> audioFeatures = new HashMap<>();
            if (node.has("audio_features")) {
                com.fasterxml.jackson.databind.JsonNode featuresNode = node.get("audio_features");
                audioFeatures.put("duration", getDoubleValue(featuresNode, "duration"));
                audioFeatures.put("speechRate", getIntValue(featuresNode, "speechRate"));
                audioFeatures.put("pauseCount", getIntValue(featuresNode, "pauseCount"));
                audioFeatures.put("pauseTotalSeconds", getDoubleValue(featuresNode, "pauseTotalSeconds"));
                audioFeatures.put("energyStability", getDoubleValue(featuresNode, "energyStability"));
                audioFeatures.put("pitchVariation", getDoubleValue(featuresNode, "pitchVariation"));
                audioFeatures.put("fillerWordCount", getIntValue(featuresNode, "fillerWordCount"));
                audioFeatures.put("emotionTendency", getTextValue(featuresNode, "emotionTendency", "unknown"));
            } else {
                audioFeatures = createEmptyFeatures();
            }
            result.put("audio_features", audioFeatures);

        } catch (Exception e) {
            result.put("text", response);
            result.put("audio_features", createEmptyFeatures());
        }

        return result;
    }

    private double getDoubleValue(com.fasterxml.jackson.databind.JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asDouble() : 0;
    }

    private int getIntValue(com.fasterxml.jackson.databind.JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asInt() : 0;
    }

    private String getTextValue(com.fasterxml.jackson.databind.JsonNode node, String field, String defaultValue) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : defaultValue;
    }
}
