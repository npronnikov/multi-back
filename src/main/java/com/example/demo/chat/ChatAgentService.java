package com.example.demo.chat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import com.example.demo.acp.AcpAgent;
import com.example.demo.acp.AcpException;
import com.example.demo.acp.AcpProperties;
import com.example.demo.acp.PromptTurnResult;
import com.example.demo.acp.TurnListener;
import com.example.demo.acp.dto.ToolCallEvent;
import com.example.demo.acp.dto.ToolCallStatusEvent;
import com.example.demo.chat.dto.ChatMessageResponse;
import com.example.demo.chat.dto.ChatSessionResponse;
import com.example.demo.chat.dto.SendMessageResult;

import jakarta.annotation.PreDestroy;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
public class ChatAgentService {

    private static final Logger log = LoggerFactory.getLogger(ChatAgentService.class);

    private final AcpAgent acpAgent;
    private final AcpProperties acpProperties;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ObjectMapper objectMapper;
    private final ExecutorService turnExecutor = Executors.newVirtualThreadPerTaskExecutor();

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
     * Persists the user message, then streams the ACP prompt turn back as newline-delimited
     * JSON (one line per thought/message chunk or tool call event, followed by a final "done"
     * line carrying the persisted assistant message). The turn itself runs on a virtual thread
     * so the request-handling thread isn't tied up for its whole duration.
     */
    public ResponseBodyEmitter streamMessage(Long sessionId, String text) {
        ChatSession session = requireSession(sessionId);
        ChatMessage userMessage = messageRepository.save(new ChatMessage(session, ChatRole.USER, text));

        ResponseBodyEmitter emitter = new ResponseBodyEmitter(0L);
        Object writeLock = new Object();
        turnExecutor.execute(() -> runTurn(emitter, writeLock, session, userMessage, text));
        return emitter;
    }

    private void runTurn(ResponseBodyEmitter emitter, Object writeLock, ChatSession session, ChatMessage userMessage, String text) {
        TurnListener listener = new TurnListener() {
            @Override
            public void onThought(String chunk) {
                sendLine(emitter, writeLock, "thought", chunk);
            }

            @Override
            public void onMessage(String chunk) {
                sendLine(emitter, writeLock, "message", chunk);
            }

            @Override
            public void onToolCall(ToolCallEvent event) {
                sendLine(emitter, writeLock, "tool_call", event);
            }

            @Override
            public void onToolCallUpdate(String toolCallId, String status) {
                sendLine(emitter, writeLock, "tool_call_update", new ToolCallStatusEvent(toolCallId, status));
            }
        };

        try {
            PromptTurnResult turnResult;
            try {
                turnResult = acpAgent.prompt(session.getAcpSessionId(), text, listener);
            } catch (RuntimeException e) {
                if (!isSessionNotFound(e)) {
                    throw e;
                }
                // The agent process was restarted since this session was created, so its
                // in-memory session table no longer knows this id - recreate it and retry once.
                String freshAcpSessionId = acpAgent.createSession(session.getCwd());
                session.setAcpSessionId(freshAcpSessionId);
                session = sessionRepository.save(session);
                turnResult = acpAgent.prompt(freshAcpSessionId, text, listener);
            }

            ChatMessage assistantMessage = new ChatMessage(session, ChatRole.ASSISTANT, turnResult.text());
            if (turnResult.thought() != null && !turnResult.thought().isBlank()) {
                assistantMessage.setThought(turnResult.thought());
            }
            if (turnResult.toolCalls() != null && !turnResult.toolCalls().isEmpty()) {
                assistantMessage.setToolCallsJson(objectMapper.writeValueAsString(turnResult.toolCalls()));
            }
            assistantMessage = messageRepository.save(assistantMessage);

            SendMessageResult result = new SendMessageResult(
                    ChatMessageResponse.from(userMessage), ChatMessageResponse.from(assistantMessage), turnResult.stopReason());
            sendLine(emitter, writeLock, "done", result);
            emitter.complete();
        } catch (Exception e) {
            log.warn("Chat turn failed for session {}", session.getId(), e);
            try {
                sendLine(emitter, writeLock, "error", new ErrorPayload(e.getMessage()));
            } catch (Exception ignored) {
                // client is already gone - nothing left to report to
            }
            emitter.completeWithError(e);
        }
    }

    private void sendLine(ResponseBodyEmitter emitter, Object writeLock, String type, Object data) {
        synchronized (writeLock) {
            try {
                ObjectNode node = objectMapper.createObjectNode();
                node.put("type", type);
                node.set("data", objectMapper.valueToTree(data));
                emitter.send(objectMapper.writeValueAsString(node) + "\n", MediaType.APPLICATION_NDJSON);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
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

    @PreDestroy
    void shutdown() {
        turnExecutor.shutdown();
    }

    private record ErrorPayload(String message) {
    }
}
