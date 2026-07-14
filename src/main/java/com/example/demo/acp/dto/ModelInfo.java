package com.example.demo.acp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelInfo(String modelId, String name, String description) {
}
