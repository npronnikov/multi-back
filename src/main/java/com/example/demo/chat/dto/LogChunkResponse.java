package com.example.demo.chat.dto;

/**
 * A slice of a turn's NDJSON log file: raw text of whatever complete lines appeared after
 * {@code nextOffset}'s predecessor, the byte offset to resume from next time, and the turn's
 * current status so the client knows when to stop polling.
 */
public record LogChunkResponse(String chunk, long nextOffset, String status) {
}
