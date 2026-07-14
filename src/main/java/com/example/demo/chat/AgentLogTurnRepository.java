package com.example.demo.chat;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface AgentLogTurnRepository extends JpaRepository<AgentLogTurn, Long> {

    List<AgentLogTurn> findBySession_IdOrderByCreatedAtDesc(Long sessionId);
}
