package com.example.demo.chat.dto;

import java.time.Instant;

import com.example.demo.chat.ChatMessage;

public record ChatMessageResponse(
        Long id, String role, String content, Instant createdAt, String thought, String toolCallsJson) {

    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getRole().name(),
                message.getContent(),
                message.getCreatedAt(),
                message.getThought(),
                message.getToolCallsJson());
    }
}
