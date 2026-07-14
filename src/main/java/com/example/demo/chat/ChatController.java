package com.example.demo.chat;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import com.example.demo.chat.dto.ChatMessageResponse;
import com.example.demo.chat.dto.ChatSessionResponse;
import com.example.demo.chat.dto.CreateSessionRequest;
import com.example.demo.chat.dto.SendMessageRequest;

/**
 * Plain request/response REST API for chatting with the ACP coding agent - no WebSocket.
 * Sending a message streams the turn back as newline-delimited JSON as it happens, rather
 * than blocking until the agent's whole reply is ready.
 */
@RestController
@RequestMapping("/api/agent/sessions")
public class ChatController {

    private final ChatAgentService chatAgentService;

    public ChatController(ChatAgentService chatAgentService) {
        this.chatAgentService = chatAgentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChatSessionResponse createSession(@RequestBody(required = false) CreateSessionRequest request) {
        String cwd = request != null ? request.cwd() : null;
        return chatAgentService.createSession(cwd);
    }

    @GetMapping
    public List<ChatSessionResponse> listSessions() {
        return chatAgentService.listSessions();
    }

    @GetMapping("/{id}/messages")
    public List<ChatMessageResponse> getMessages(@PathVariable Long id) {
        return chatAgentService.getMessages(id);
    }

    @PostMapping(value = "/{id}/messages", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public ResponseBodyEmitter sendMessage(@PathVariable Long id, @RequestBody SendMessageRequest request) {
        return chatAgentService.streamMessage(id, request.text());
    }

    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void cancel(@PathVariable Long id) {
        chatAgentService.cancel(id);
    }
}
