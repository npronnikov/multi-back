package com.example.demo.acp.dto;

import java.util.List;

public record NewSessionParams(String cwd, List<Object> mcpServers) {
}
