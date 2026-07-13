package com.example.demo.chat.dto;

/** cwd is optional; when absent the server-configured default agent working directory is used. */
public record CreateSessionRequest(String cwd) {
}
