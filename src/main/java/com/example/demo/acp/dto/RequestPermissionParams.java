package com.example.demo.acp.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.databind.JsonNode;

/** Params of the agent -> client "session/request_permission" request. toolCall is left untyped; this client only inspects it for logging. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RequestPermissionParams(String sessionId, JsonNode toolCall, List<PermissionOption> options) {
}
