package com.example.authbackend.service;

import com.example.authbackend.entity.KnowledgeBase;
import com.example.authbackend.repository.KnowledgeBaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class KnowledgeBaseService {

    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Autowired
    private RagService ragService;

    @Autowired
    private DocumentChunkingService chunkingService;

    private static final String KNOWLEDGE_COLLECTION_NAME = "kb_vector";

    @Transactional
    public Map<String, Object> addKnowledge(String title, String content, String category, Long creatorId) {
        KnowledgeBase knowledge = new KnowledgeBase();
        knowledge.setTitle(title);
        knowledge.setContent(content);
        knowledge.setCategory(category);
        knowledge.setCreatorId(creatorId);
        knowledge.setStatus("ACTIVE");

        knowledge = knowledgeBaseRepository.save(knowledge);

        try {
            indexKnowledgeToVectorStore(knowledge);
        } catch (Exception e) {
            System.err.println("索引创建失败，但知识库内容已保存: " + e.getMessage());
            knowledge.setStatus("INDEX_FAILED");
            knowledge.setChunkCount(0);
            knowledge = knowledgeBaseRepository.save(knowledge);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", buildKnowledgeResponse(knowledge));
        return result;
    }

    @Transactional
    public Map<String, Object> updateKnowledge(Long id, String title, String content, String category, Long creatorId) {
        KnowledgeBase knowledge = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("知识库条目不存在"));

        if (!knowledge.getCreatorId().equals(creatorId)) {
            throw new RuntimeException("无权修改此条目");
        }

        knowledge.setTitle(title);
        knowledge.setContent(content);
        knowledge.setCategory(category);

        knowledge = knowledgeBaseRepository.save(knowledge);

        deleteFromIndex(id);
        indexKnowledgeToVectorStore(knowledge);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", buildKnowledgeResponse(knowledge));
        return result;
    }

    @Transactional
    public void deleteKnowledge(Long id, Long creatorId) {
        KnowledgeBase knowledge = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("知识库条目不存在"));

        if (!knowledge.getCreatorId().equals(creatorId)) {
            throw new RuntimeException("无权删除此条目");
        }

        deleteFromIndex(id);
        knowledgeBaseRepository.delete(knowledge);
    }

    public Map<String, Object> getKnowledgeList(String category, String status, int page, int size) {
        List<KnowledgeBase> knowledgeList;

        if (category != null && !category.isEmpty() && status != null && !status.isEmpty()) {
            knowledgeList = knowledgeBaseRepository.findByStatusAndCategoryOrderByCreatedAtDesc(status, category);
        } else if (category != null && !category.isEmpty()) {
            knowledgeList = knowledgeBaseRepository.findByCategoryOrderByCreatedAtDesc(category);
        } else if (status != null && !status.isEmpty()) {
            knowledgeList = knowledgeBaseRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            knowledgeList = knowledgeBaseRepository.findAll();
        }

        int start = page * size;
        int end = Math.min(start + size, knowledgeList.size());
        if (start >= knowledgeList.size()) {
            start = knowledgeList.size();
        }
        List<KnowledgeBase> pagedList = knowledgeList.subList(start, end);

        List<Map<String, Object>> items = new ArrayList<>();
        for (KnowledgeBase kb : pagedList) {
            items.add(buildKnowledgeResponse(kb));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", Map.of(
            "items", items,
            "total", knowledgeList.size(),
            "page", page,
            "size", size
        ));
        return result;
    }

    public Map<String, Object> getKnowledgeById(Long id) {
        KnowledgeBase knowledge = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("知识库条目不存在"));

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", buildKnowledgeResponse(knowledge));
        return result;
    }

    public List<Map<String, Object>> getCategories() {
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(Map.of("name", "Java后端", "count", countByCategory("Java后端")));
        result.add(Map.of("name", "Web前端", "count", countByCategory("Web前端")));
        return result;
    }

    public Map<String, Object> getStatistics() {
        long totalCount = knowledgeBaseRepository.count();
        long activeCount = knowledgeBaseRepository.countByStatus("ACTIVE");

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", totalCount);
        stats.put("activeCount", activeCount);
        return stats;
    }

    @Transactional
    public Map<String, Object> reindexKnowledge(Long id) {
        KnowledgeBase knowledge = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("知识库条目不存在"));

        deleteFromIndex(id);
        indexKnowledgeToVectorStore(knowledge);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "重建索引成功");
        return result;
    }

    private void indexKnowledgeToVectorStore(KnowledgeBase knowledge) {
        try {
            List<String> chunks = chunkingService.chunkText(knowledge.getContent());

            knowledge.setChunkCount(chunks.size());
            knowledge.setIndexedAt(LocalDateTime.now());
            knowledgeBaseRepository.save(knowledge);

            if (chunks.isEmpty()) {
                return;
            }

            for (int i = 0; i < chunks.size(); i++) {
                String chunkContent = chunks.get(i);
                try {
                    String embedding = ragService.getEmbedding(chunkContent);

                    Map<String, Object> document = new HashMap<>();
                    document.put("id", "kb_" + knowledge.getId() + "_" + i);
                    document.put("embedding", parseEmbedding(embedding));
                    document.put("document", chunkContent);
                    document.put("metadata", Map.of(
                        "knowledgeId", knowledge.getId(),
                        "title", knowledge.getTitle(),
                        "category", knowledge.getCategory(),
                        "chunkIndex", i,
                        "source", "knowledge_base"
                    ));

                    ragService.addDocumentToCollection(KNOWLEDGE_COLLECTION_NAME, document);
                } catch (Exception e) {
                    System.err.println("Failed to index chunk " + i + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to index knowledge base: " + e.getMessage());
            throw new RuntimeException("索引创建失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }

    private void deleteFromIndex(Long knowledgeId) {
        try {
            ragService.deleteByKnowledgeId(KNOWLEDGE_COLLECTION_NAME, knowledgeId);
        } catch (Exception e) {
            System.err.println("Failed to delete from index: " + e.getMessage());
        }
    }

    private int countByCategory(String category) {
        return knowledgeBaseRepository.findByCategoryOrderByCreatedAtDesc(category).size();
    }

    private List<Double> parseEmbedding(String embeddingJson) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(embeddingJson, List.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse embedding: " + e.getMessage());
        }
    }

    private Map<String, Object> buildKnowledgeResponse(KnowledgeBase knowledge) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", knowledge.getId());
        response.put("title", knowledge.getTitle());
        response.put("content", knowledge.getContent());
        response.put("category", knowledge.getCategory());
        response.put("creatorId", knowledge.getCreatorId());
        response.put("status", knowledge.getStatus());
        response.put("chunkCount", knowledge.getChunkCount());
        response.put("indexedAt", knowledge.getIndexedAt() != null ? knowledge.getIndexedAt().toString() : null);
        response.put("createdAt", knowledge.getCreatedAt() != null ? knowledge.getCreatedAt().toString() : null);
        response.put("updatedAt", knowledge.getUpdatedAt() != null ? knowledge.getUpdatedAt().toString() : null);
        return response;
    }

    public String retrieveFromKnowledgeBase(String query, int topK) {
        return ragService.queryKnowledgeBase(query, topK);
    }
}