package com.example.authbackend.repository;

import com.example.authbackend.entity.ExcellentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExcellentAnswerRepository extends JpaRepository<ExcellentAnswer, Long> {

    List<ExcellentAnswer> findByStatusOrderByCreatedAtDesc(String status);

    List<ExcellentAnswer> findByCategoryAndStatusOrderByCreatedAtDesc(String category, String status);

    @Query("SELECT DISTINCT e.category FROM ExcellentAnswer e WHERE e.status = 'ACTIVE'")
    List<String> findDistinctCategories();

    long countByStatus(String status);
}