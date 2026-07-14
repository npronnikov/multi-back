package com.example.demo.acp.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionModelState(List<ModelInfo> availableModels, String currentModelId) {
}
