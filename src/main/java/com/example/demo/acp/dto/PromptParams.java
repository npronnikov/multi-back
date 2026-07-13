package com.example.demo.acp.dto;

import java.util.List;

/** Note: the ACP wire field is named "prompt", not "content" as some docs suggest. */
public record PromptParams(String sessionId, List<ContentBlock> prompt) {
}
