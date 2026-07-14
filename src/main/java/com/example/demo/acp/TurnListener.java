package com.example.demo.acp;

import com.example.demo.acp.dto.ToolCallEvent;

/** Callback invoked as a prompt turn progresses, so callers can stream it out incrementally. */
public interface TurnListener {

    TurnListener NOOP = new TurnListener() {
    };

    default void onThought(String text) {
    }

    default void onMessage(String text) {
    }

    default void onToolCall(ToolCallEvent event) {
    }

    default void onToolCallUpdate(String toolCallId, String status) {
    }
}
