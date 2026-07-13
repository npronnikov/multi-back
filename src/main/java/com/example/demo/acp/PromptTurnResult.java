package com.example.demo.acp;

import java.util.List;

import com.example.demo.acp.dto.ToolCallEvent;

/** The outcome of a single completed session/prompt turn: the concatenated agent reply and why it stopped. */
public record PromptTurnResult(String text, String stopReason, String thought, List<ToolCallEvent> toolCalls) {
}
