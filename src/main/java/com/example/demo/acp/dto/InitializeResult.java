package com.example.demo.acp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Only the fields the client actually acts on are modeled; the rest of the handshake payload is ignored. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InitializeResult(int protocolVersion) {
}
