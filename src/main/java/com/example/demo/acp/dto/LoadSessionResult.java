package com.example.demo.acp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LoadSessionResult(SessionModelState models) {
}
