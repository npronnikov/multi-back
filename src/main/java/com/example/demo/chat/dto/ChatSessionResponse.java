package com.example.demo.chat.dto;

import java.time.Instant;

import com.example.demo.chat.ChatSession;

public record ChatSessionResponse(
        Long id, String cwd, Instant createdAt, String currentModelId, String availableModelsJson) {

    public static ChatSessionResponse from(ChatSession session) {
        return new ChatSessionResponse(
                session.getId(),
                session.getCwd(),
                session.getCreatedAt(),
                session.getCurrentModelId(),
                session.getAvailableModelsJson());
    }
}
