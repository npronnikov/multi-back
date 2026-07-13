package com.example.demo.acp.dto;

/** A tool call surfaced during a prompt turn, with its most recently reported status. */
public record ToolCallEvent(String toolCallId, String title, String kind, String status) {
}
