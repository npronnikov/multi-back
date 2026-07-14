package com.example.demo.chat.dto;

import java.time.Instant;

import com.example.demo.chat.AgentLogTurn;

public record AgentLogTurnResponse(Long turnId, String status, String prompt, Instant createdAt) {

    public static AgentLogTurnResponse from(AgentLogTurn turn) {
        return new AgentLogTurnResponse(turn.getId(), turn.getStatus().name(), turn.getPrompt(), turn.getCreatedAt());
    }
}
