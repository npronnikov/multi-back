package com.example.demo.chat;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.acp.AcpAgent;
import com.example.demo.acp.AcpException;
import com.example.demo.acp.AcpProperties;
import com.example.demo.acp.PromptTurnResult;
import com.example.demo.chat.dto.ChatMessageResponse;
import com.example.demo.chat.dto.ChatSessionResponse;
import com.example.demo.chat.dto.SendMessageResult;

import tools.jackson.databind.ObjectMapper;

@Service
public class ChatAgentService {

    private final AcpAgent acpAgent;
    private final AcpProperties acpProperties;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    public ChatAgentService(AcpAgent acpAgent,
                             AcpProperties acpProperties,
                             ChatSessionRepository sessionRepository,
                             ChatMessageRepository messageRepository,
                             ObjectMapper objectMapper) {
        this.acpAgent = acpAgent;
        this.acpProperties = acpProperties;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.objectMapper = objectMapper;
    }

    public ChatSessionResponse createSession(String cwd) {
        String effectiveCwd = (cwd == null || cwd.isBlank()) ? acpProperties.workingDirectory() : cwd;
        String acpSessionId = acpAgent.createSession(effectiveCwd);
        ChatSession session = sessionRepository.save(new ChatSession(acpSessionId, effectiveCwd));
        return ChatSessionResponse.from(session);
    }

    public List<ChatSessionResponse> listSessions() {
        return sessionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ChatSessionResponse::from)
                .toList();
    }

    public List<ChatMessageResponse> getMessages(Long sessionId) {
        requireSession(sessionId);
        return messageRepository.findBySession_IdOrderByIdAsc(sessionId).stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    /**
     * Persists the user message, blocks on the ACP prompt turn, then persists the assistant reply.
     * Deliberately not wrapped in a single transaction: the ACP call can take a long time and must
     * not hold a database transaction open for the duration.
     */
    public SendMessageResult sendMessage(Long sessionId, String text) {
        ChatSession session = requireSession(sessionId);
        ChatMessage userMessage = messageRepository.save(new ChatMessage(session, ChatRole.USER, text));

        PromptTurnResult turnResult;
        try {
            turnResult = acpAgent.prompt(session.getAcpSessionId(), text);
        } catch (RuntimeException e) {
            if (!isSessionNotFound(e)) {
                throw e;
            }
            // The agent process was restarted since this session was created, so its
            // in-memory session table no longer knows this id - recreate it and retry once.
            String freshAcpSessionId = acpAgent.createSession(session.getCwd());
            session.setAcpSessionId(freshAcpSessionId);
            session = sessionRepository.save(session);
            turnResult = acpAgent.prompt(freshAcpSessionId, text);
        }

        ChatMessage assistantMessage = new ChatMessage(session, ChatRole.ASSISTANT, turnResult.text());
        if (turnResult.thought() != null && !turnResult.thought().isBlank()) {
            assistantMessage.setThought(turnResult.thought());
        }
        if (turnResult.toolCalls() != null && !turnResult.toolCalls().isEmpty()) {
            assistantMessage.setToolCallsJson(objectMapper.writeValueAsString(turnResult.toolCalls()));
        }
        assistantMessage = messageRepository.save(assistantMessage);

        return new SendMessageResult(
                ChatMessageResponse.from(userMessage),
                ChatMessageResponse.from(assistantMessage),
                turnResult.stopReason());
    }

    public void cancel(Long sessionId) {
        ChatSession session = requireSession(sessionId);
        acpAgent.cancel(session.getAcpSessionId());
    }

    private ChatSession requireSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private static boolean isSessionNotFound(RuntimeException e) {
        Throwable cause = e instanceof java.util.concurrent.CompletionException && e.getCause() != null ? e.getCause() : e;
        if (!(cause instanceof AcpException)) {
            return false;
        }
        String message = cause.getMessage();
        return message != null && message.contains("Session not found");
    }
}
