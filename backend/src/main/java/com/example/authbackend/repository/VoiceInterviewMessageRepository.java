package com.example.authbackend.repository;

import com.example.authbackend.entity.VoiceInterviewMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VoiceInterviewMessageRepository extends JpaRepository<VoiceInterviewMessage, Long> {
    List<VoiceInterviewMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    void deleteBySessionId(Long sessionId);
}