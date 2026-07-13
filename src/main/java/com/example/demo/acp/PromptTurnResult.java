package com.example.demo.acp;

/** The outcome of a single completed session/prompt turn: the concatenated agent reply and why it stopped. */
public record PromptTurnResult(String text, String stopReason) {
}
