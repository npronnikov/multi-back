package com.example.demo.acp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Params of the agent -> client "session/update" notification. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionUpdateNotification(String sessionId, SessionUpdate update) {
}
