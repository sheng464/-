package com.example.authbackend.repository;

import com.example.authbackend.entity.TextInterviewMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TextInterviewMessageRepository extends JpaRepository<TextInterviewMessage, Long> {
    List<TextInterviewMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    void deleteBySessionId(Long sessionId);
}