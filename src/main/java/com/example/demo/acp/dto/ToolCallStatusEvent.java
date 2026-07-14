package com.example.demo.acp.dto;

/** Streamed over SSE when a previously reported tool call's status changes. */
public record ToolCallStatusEvent(String toolCallId, String status) {
}
