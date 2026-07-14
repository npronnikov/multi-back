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
import com.example.demo.acp.AcpProperties;
import com.example.demo.acp.PromptTurnResult;
import com.example.demo.acp.RecoveredSession;
import com.example.demo.acp.TurnListener;
import com.example.demo.acp.dto.NewSessionResult;
import com.example.demo.acp.dto.SessionModelState;
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
        NewSessionResult result = acpAgent.createSession(effectiveCwd);
        ChatSession session = new ChatSession(result.sessionId(), effectiveCwd);
        applyModelState(session, result.models());
        session = sessionRepository.save(session);
        return ChatSessionResponse.from(session);
    }

    /** Switches the model an existing session's turns will use going forward. */
    public ChatSessionResponse setModel(Long sessionId, String modelId) {
        ChatSession session = requireSession(sessionId);
        try {
            acpAgent.setModel(session.getAcpSessionId(), modelId);
        } catch (RuntimeException e) {
            if (!AcpAgent.isSessionNotFoundError(e)) {
                throw agentError(e);
            }
            // The agent process was restarted since this session was created, so it no longer
            // recognizes this id - recover it (reload from disk if possible) and retry once.
            RecoveredSession recovered = acpAgent.recoverSession(session.getCwd(), session.getAcpSessionId());
            session.setAcpSessionId(recovered.sessionId());
            applyModelState(session, recovered.models());
            try {
                acpAgent.setModel(recovered.sessionId(), modelId);
            } catch (RuntimeException retryFailure) {
                throw agentError(retryFailure);
            }
        }
        session.setCurrentModelId(modelId);
        session = sessionRepository.save(session);
        return ChatSessionResponse.from(session);
    }

    private ResponseStatusException agentError(RuntimeException e) {
        String agentMessage = AcpAgent.agentErrorMessage(e);
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, agentMessage != null ? agentMessage : "Failed to switch model", e);
    }

    private void applyModelState(ChatSession session, SessionModelState models) {
        if (models == null) {
            return;
        }
        session.setCurrentModelId(models.currentModelId());
        session.setAvailableModelsJson(objectMapper.writeValueAsString(models.availableModels()));
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
                if (!AcpAgent.isSessionNotFoundError(e)) {
                    throw e;
                }
                // The agent process was restarted since this session was created, so it no longer
                // recognizes this id - recover it (reload from disk if possible) and retry once.
                RecoveredSession recovered = acpAgent.recoverSession(session.getCwd(), session.getAcpSessionId());
                session.setAcpSessionId(recovered.sessionId());
                applyModelState(session, recovered.models());
                session = sessionRepository.save(session);
                turnResult = acpAgent.prompt(recovered.sessionId(), text, listener);
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

    @PreDestroy
    void shutdown() {
        turnExecutor.shutdown();
    }

    private record ErrorPayload(String message) {
    }
}
