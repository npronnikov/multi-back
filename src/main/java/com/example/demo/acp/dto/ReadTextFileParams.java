package com.example.demo.acp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** line is the 1-based first line to read; limit is the max number of lines. Both are optional. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReadTextFileParams(String sessionId, String path, Integer line, Integer limit) {
}
