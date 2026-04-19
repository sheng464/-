package com.example.authbackend.repository;

import com.example.authbackend.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {

    List<KnowledgeBase> findByStatusOrderByCreatedAtDesc(String status);

    List<KnowledgeBase> findByCategoryOrderByCreatedAtDesc(String category);

    List<KnowledgeBase> findByCreatorIdOrderByCreatedAtDesc(Long creatorId);

    List<KnowledgeBase> findByStatusAndCategoryOrderByCreatedAtDesc(String status, String category);

    long countByStatus(String status);
}