package com.example.demo.chat.dto;

import java.time.Instant;

import com.example.demo.chat.ChatSession;

public record ChatSessionResponse(Long id, String cwd, Instant createdAt) {

    public static ChatSessionResponse from(ChatSession session) {
        return new ChatSessionResponse(session.getId(), session.getCwd(), session.getCreatedAt());
    }
}
