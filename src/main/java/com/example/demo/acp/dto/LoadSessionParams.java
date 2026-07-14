package com.example.demo.acp.dto;

import java.util.List;

public record LoadSessionParams(String cwd, List<Object> mcpServers, String sessionId) {
}
