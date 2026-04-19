package com.example.authbackend.repository;

import com.example.authbackend.entity.AbilityEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AbilityEvaluationRepository extends JpaRepository<AbilityEvaluation, Long> {
    List<AbilityEvaluation> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<AbilityEvaluation> findBySessionId(Long sessionId);
    void deleteBySessionId(Long sessionId);
}