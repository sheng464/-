package com.example.authbackend.service;

import com.example.authbackend.config.VectorStoreConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
public class RagService {

    @Autowired
    private VectorStoreConfig config;

    @Autowired
    private DocumentChunkingService chunkingService;

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TENANT = "default_tenant";
    private static final String DATABASE = "default_database";

    private final Map<String, String> collectionIdCache = new HashMap<>();

    public RagService(RestClient.Builder restClientBuilder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(60000);
        this.restClient = restClientBuilder
                .requestFactory(factory)
                .build();
    }

    private String getCollectionId(String collectionName) {
        if (collectionIdCache.containsKey(collectionName)) {
            return collectionIdCache.get(collectionName);
        }
        try {
            String response = restClient.get()
                .uri(config.getChromaUrl() + "/api/v2/tenants/" + TENANT + "/databases/" + DATABASE + "/collections/" + collectionName)
                .retrieve()
                .body(String.class);
            JsonNode node = objectMapper.readTree(response);
            String collectionId = node.get("id").asText();
            collectionIdCache.put(collectionName, collectionId);
            return collectionId;
        } catch (Exception e) {
            String collectionId = createCollection(collectionName);
            collectionIdCache.put(collectionName, collectionId);
            return collectionId;
        }
    }

    private String createCollection(String collectionName) {
        try {
            Map<String, Object> requestBody = Map.of(
                "name", collectionName
            );

            String response = restClient.post()
                .uri(config.getChromaUrl() + "/api/v2/tenants/" + TENANT + "/databases/" + DATABASE + "/collections")
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(String.class);

            JsonNode node = objectMapper.readTree(response);
            return node.get("id").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create collection: " + e.getMessage());
        }
    }

    @Async
    public void indexResume(Long resumeId, Long userId, String resumeText) {
        List<String> chunks = chunkingService.chunkResumeBySections(resumeText);
        String colId = getCollectionId("resume_collection");

        for (int i = 0; i < chunks.size(); i++) {
            String chunkContent = chunks.get(i);
            try {
                String embedding = getEmbedding(chunkContent);

                List<String> ids = new ArrayList<>();
                ids.add(resumeId + "_" + i);

                List<List<Double>> embeddings = new ArrayList<>();
                embeddings.add(objectMapper.readValue(embedding, List.class));

                List<String> documents = new ArrayList<>();
                documents.add(chunkContent);

                List<Map<String, Object>> metadatas = new ArrayList<>();
                metadatas.add(Map.of(
                    "resumeId", resumeId,
                    "userId", userId,
                    "chunkIndex", i,
                    "source", "resume"
                ));

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("ids", ids);
                requestBody.put("embeddings", embeddings);
                requestBody.put("documents", documents);
                requestBody.put("metadatas", metadatas);

                restClient.post()
                    .uri(config.getChromaUrl() + "/api/v2/tenants/" + TENANT + "/databases/" + DATABASE + "/collections/" + colId + "/add")
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
            } catch (Exception e) {
                System.err.println("Failed to index chunk " + i + ": " + e.getMessage());
            }
        }
    }

    public String retrieveRelevantContext(String query, int topK, Long userId) {
        try {
            String queryEmbedding = getEmbedding(query);
            List<?> embeddingList = objectMapper.readValue(queryEmbedding, List.class);
            String colId = getCollectionId("resume_collection");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query_embeddings", List.of(embeddingList));
            requestBody.put("nResults", topK);
            requestBody.put("where", Map.of("userId", userId));
            requestBody.put("include", List.of("documents", "metadatas", "distances"));

            String response = restClient.post()
                .uri(config.getChromaUrl() + "/api/v2/tenants/" + TENANT + "/databases/" + DATABASE + "/collections/" + colId + "/query")
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(String.class);

            return parseQueryResponse(response);
        } catch (Exception e) {
            System.err.println("Failed to retrieve context: " + e.getMessage());
            return "";
        }
    }

    public String retrieveRelevantContext(String query, int topK) {
        try {
            String queryEmbedding = getEmbedding(query);
            List<?> embeddingList = objectMapper.readValue(queryEmbedding, List.class);
            String colId = getCollectionId("resume_collection");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query_embeddings", List.of(embeddingList));
            requestBody.put("nResults", topK);
            requestBody.put("include", List.of("documents", "metadatas", "distances"));

            String response = restClient.post()
                .uri(config.getChromaUrl() + "/api/v2/tenants/" + TENANT + "/databases/" + DATABASE + "/collections/" + colId + "/query")
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(String.class);

            return parseQueryResponse(response);
        } catch (Exception e) {
            System.err.println("Failed to retrieve context: " + e.getMessage());
            return "";
        }
    }

    public String buildContextFromDocuments(String documentsText) {
        if (documentsText == null || documentsText.isEmpty() || documentsText.trim().isEmpty()) {
            return "";
        }
        return "【相关简历内容】\n\n" + documentsText.trim() + "\n";
    }

    public void addDocumentToCollection(String collectionName, Map<String, Object> document) {
        try {
            String colId = getCollectionId(collectionName);

            List<String> ids = new ArrayList<>();
            ids.add((String) document.get("id"));

            List<List<Double>> embeddings = new ArrayList<>();
            embeddings.add((List<Double>) document.get("embedding"));

            List<String> documents = new ArrayList<>();
            documents.add((String) document.get("document"));

            List<Map<String, Object>> metadatas = new ArrayList<>();
            metadatas.add((Map<String, Object>) document.get("metadata"));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("ids", ids);
            requestBody.put("embeddings", embeddings);
            requestBody.put("documents", documents);
            requestBody.put("metadatas", metadatas);

            restClient.post()
                .uri(config.getChromaUrl() + "/api/v2/tenants/" + TENANT + "/databases/" + DATABASE + "/collections/" + colId + "/add")
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(String.class);
        } catch (Exception e) {
            System.err.println("Failed to add document to collection: " + e.getMessage());
            throw new RuntimeException("Failed to add document: " + e.getMessage());
        }
    }

    public void deleteByKnowledgeId(String collectionName, Long knowledgeId) {
        try {
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                ids.add("kb_" + knowledgeId + "_" + i);
            }

            Map<String, Object> requestBody = Map.of("ids", ids);
            String colId = getCollectionId(collectionName);

            restClient.post()
                .uri(config.getChromaUrl() + "/api/v2/tenants/" + TENANT + "/databases/" + DATABASE + "/collections/" + colId + "/delete")
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(String.class);
        } catch (Exception e) {
            System.err.println("Failed to delete knowledge base from index: " + e.getMessage());
        }
    }

    public String queryKnowledgeBase(String query, int topK) {
        return queryCollection("kb_vector", query, topK);
    }

    public String queryCollection(String collectionName, String query, int topK) {
        try {
            String queryEmbedding = getEmbedding(query);
            List<?> embeddingList = objectMapper.readValue(queryEmbedding, List.class);
            String colId = getCollectionId(collectionName);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query_embeddings", List.of(embeddingList));
            requestBody.put("nResults", topK);
            requestBody.put("include", List.of("documents", "metadatas", "distances"));

            String response = restClient.post()
                .uri(config.getChromaUrl() + "/api/v2/tenants/" + TENANT + "/databases/" + DATABASE + "/collections/" + colId + "/query")
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(String.class);

            return parseQueryResponse(response);
        } catch (Exception e) {
            System.err.println("Failed to query collection " + collectionName + ": " + e.getMessage());
            return "";
        }
    }

    public String getEmbedding(String text) {
        Map<String, Object> requestBody = Map.of(
            "model", config.getEmbeddingModel(),
            "prompt", text
        );

        String response = restClient.post()
            .uri(config.getOllamaUrl() + "/api/embeddings")
            .header("Content-Type", "application/json")
            .body(requestBody)
            .retrieve()
            .body(String.class);

        try {
            JsonNode node = objectMapper.readTree(response);
            return node.get("embedding").toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse embedding response: " + e.getMessage());
        }
    }

    private String parseQueryResponse(String response) {
        try {
            JsonNode node = objectMapper.readTree(response);
            JsonNode documents = node.get("documents");
            JsonNode distances = node.get("distances");

            if (documents != null && documents.isArray() && documents.size() > 0) {
                JsonNode firstResultDocs = documents.get(0);
                JsonNode firstResultDistances = distances != null ? distances.get(0) : null;

                if (firstResultDocs.isArray() && firstResultDocs.size() > 0) {
                    double bestDistance = firstResultDistances != null && firstResultDistances.isArray()
                        ? firstResultDistances.get(0).asDouble()
                        : 0.0;

                    if (bestDistance > config.getSimilarityThreshold()) {
                        return "";
                    }

                    StringBuilder sb = new StringBuilder();
                    int count = 0;
                    for (int i = 0; i < firstResultDocs.size(); i++) {
                        double distance = firstResultDistances != null && firstResultDistances.isArray()
                            ? firstResultDistances.get(i).asDouble()
                            : 0.0;

                        if (distance > 0 && distance <= config.getSimilarityThreshold()) {
                            sb.append(firstResultDocs.get(i).asText()).append("\n\n");
                            count++;
                        }
                    }
                    return count > 0 ? sb.toString().trim() : "";
                }
            }
            return "";
        } catch (Exception e) {
            System.err.println("Failed to parse query response: " + e.getMessage());
            return "";
        }
    }

    public void deleteResumeFromIndex(Long resumeId) {
        try {
            Map<String, Object> requestBody = Map.of(
                "ids", List.of(resumeId + "_0", resumeId + "_1", resumeId + "_2", resumeId + "_3", resumeId + "_4")
            );
            String colId = getCollectionId("resume_collection");

            restClient.post()
                .uri(config.getChromaUrl() + "/api/v2/tenants/" + TENANT + "/databases/" + DATABASE + "/collections/" + colId + "/delete")
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(String.class);
        } catch (Exception e) {
            System.err.println("Failed to delete resume from index: " + e.getMessage());
        }
    }
}
