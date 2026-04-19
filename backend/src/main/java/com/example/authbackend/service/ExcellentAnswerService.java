package com.example.authbackend.service;

import com.example.authbackend.entity.ExcellentAnswer;
import com.example.authbackend.repository.ExcellentAnswerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ExcellentAnswerService {

    private static final String EXCELLENT_COLLECTION_NAME = "excellent_vector";

    @Autowired
    private ExcellentAnswerRepository excellentAnswerRepository;

    @Autowired
    private RagService ragService;

    @Autowired
    private DocumentChunkingService chunkingService;

    @Transactional
    public ExcellentAnswer addExcellentAnswer(String title, String content, String category, Long userId) {
        ExcellentAnswer excellentAnswer = new ExcellentAnswer();
        excellentAnswer.setTitle(title);
        excellentAnswer.setContent(content);
        excellentAnswer.setCategory(category);
        excellentAnswer.setCreatorId(userId);
        excellentAnswer.setStatus("ACTIVE");
        excellentAnswer.setScore(100);

        ExcellentAnswer saved = excellentAnswerRepository.save(excellentAnswer);

        indexToVectorStore(saved);

        return saved;
    }

    private void indexToVectorStore(ExcellentAnswer excellentAnswer) {
        try {
            String content = excellentAnswer.getTitle() + "\n\n" + excellentAnswer.getContent();
            List<String> chunks = chunkingService.chunkText(content);

            for (int i = 0; i < chunks.size(); i++) {
                String chunkContent = chunks.get(i);
                String embedding = ragService.getEmbedding(chunkContent);

                Map<String, Object> document = new HashMap<>();
                document.put("id", "ea_" + excellentAnswer.getId() + "_" + i);
                document.put("embedding", parseEmbedding(embedding));
                document.put("document", chunkContent);
                document.put("metadata", Map.of(
                    "excellentAnswerId", excellentAnswer.getId(),
                    "title", excellentAnswer.getTitle(),
                    "content", excellentAnswer.getContent(),
                    "category", excellentAnswer.getCategory(),
                    "score", excellentAnswer.getScore() != null ? excellentAnswer.getScore() : 100
                ));

                ragService.addDocumentToCollection(EXCELLENT_COLLECTION_NAME, document);
            }

            excellentAnswer.setChunkCount(chunks.size());
            excellentAnswer.setIndexedAt(LocalDateTime.now());
            excellentAnswerRepository.save(excellentAnswer);

        } catch (Exception e) {
            System.err.println("Failed to index excellent answer: " + e.getMessage());
            excellentAnswer.setStatus("INDEX_FAILED");
            excellentAnswerRepository.save(excellentAnswer);
        }
    }

    private List<Double> parseEmbedding(String embeddingJson) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<?> list = mapper.readValue(embeddingJson, List.class);
            List<Double> result = new ArrayList<>();
            for (Object v : list) {
                if (v instanceof Number) {
                    result.add(((Number) v).doubleValue());
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse embedding: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getExcellentAnswers(int page, int size) {
        List<ExcellentAnswer> all = excellentAnswerRepository.findByStatusOrderByCreatedAtDesc("ACTIVE");

        int start = page * size;
        int end = Math.min(start + size, all.size());
        if (start >= all.size()) {
            return new ArrayList<>();
        }

        List<ExcellentAnswer> paged = all.subList(start, end);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ExcellentAnswer ea : paged) {
            result.add(buildResponse(ea));
        }
        return result;
    }

    public Map<String, Object> getStatistics() {
        long totalCount = excellentAnswerRepository.count();
        long activeCount = excellentAnswerRepository.countByStatus("ACTIVE");
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", totalCount);
        stats.put("activeCount", activeCount);
        return stats;
    }

    public List<String> getCategories() {
        return excellentAnswerRepository.findDistinctCategories();
    }

    public List<Map<String, Object>> searchByCategory(String category) {
        List<ExcellentAnswer> list = excellentAnswerRepository.findByCategoryAndStatusOrderByCreatedAtDesc(category, "ACTIVE");
        List<Map<String, Object>> result = new ArrayList<>();
        for (ExcellentAnswer ea : list) {
            result.add(buildResponse(ea));
        }
        return result;
    }

    @Transactional
    public void deleteExcellentAnswer(Long id) {
        try {
            ragService.deleteByKnowledgeId(EXCELLENT_COLLECTION_NAME, id);
        } catch (Exception e) {
            System.err.println("Failed to delete from vector store: " + e.getMessage());
        }
        excellentAnswerRepository.deleteById(id);
    }

    public String retrieveRelevantExcellentAnswers(String query, int topK) {
        try {
            String results = ragService.queryCollection(EXCELLENT_COLLECTION_NAME, query, topK);
            return results;
        } catch (Exception e) {
            System.err.println("Failed to retrieve excellent answers: " + e.getMessage());
            return "";
        }
    }

    private Map<String, Object> buildResponse(ExcellentAnswer ea) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", ea.getId());
        map.put("title", ea.getTitle());
        map.put("content", ea.getContent());
        map.put("category", ea.getCategory());
        map.put("score", ea.getScore());
        map.put("status", ea.getStatus());
        map.put("chunkCount", ea.getChunkCount());
        map.put("createdAt", ea.getCreatedAt() != null ? ea.getCreatedAt().toString() : "");
        map.put("updatedAt", ea.getUpdatedAt() != null ? ea.getUpdatedAt().toString() : "");
        return map;
    }
}