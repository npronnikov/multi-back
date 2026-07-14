package com.example.demo.acp;

import com.example.demo.acp.dto.SessionModelState;

/** Result of {@link AcpAgent#recoverSession}: the session id to keep using, and its model state. */
public record RecoveredSession(String sessionId, SessionModelState models) {
}
