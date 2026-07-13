package com.example.demo.chat.dto;

public record SendMessageResult(ChatMessageResponse userMessage, ChatMessageResponse assistantMessage, String stopReason) {
}
