package com.example.demo.acp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.demo.acp.dto.ContentBlock;
import com.example.demo.acp.dto.InitializeParams;
import com.example.demo.acp.dto.InitializeResult;
import com.example.demo.acp.dto.NewSessionParams;
import com.example.demo.acp.dto.NewSessionResult;
import com.example.demo.acp.dto.PromptParams;
import com.example.demo.acp.dto.PromptResult;
import com.example.demo.acp.dto.ReadTextFileParams;
import com.example.demo.acp.dto.ReadTextFileResult;
import com.example.demo.acp.dto.RequestPermissionParams;
import com.example.demo.acp.dto.RequestPermissionResult;
import com.example.demo.acp.dto.SessionUpdate;
import com.example.demo.acp.dto.SessionUpdateNotification;
import com.example.demo.acp.dto.ToolCallEvent;
import com.example.demo.acp.dto.WriteTextFileParams;
import com.example.demo.acp.dto.WriteTextFileResult;

import jakarta.annotation.PreDestroy;
import tools.jackson.databind.ObjectMapper;

/**
 * Domain-level wrapper around {@link AcpClient} that speaks the specific subset of the
 * Agent Client Protocol this application needs: a handshake, session creation, prompt
 * turns (with their streamed reply reassembled), cancellation, and the client-side
 * callbacks an agent expects (file access, permission requests).
 *
 * <p>The underlying agent process is started lazily, on first use, and reused for the
 * lifetime of the application.
 */
@Component
public class AcpAgent {

    private static final Logger log = LoggerFactory.getLogger(AcpAgent.class);
    private static final int PROTOCOL_VERSION = 1;

    private final AcpProperties properties;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, TurnAccumulator> turns = new ConcurrentHashMap<>();
    private volatile AcpClient client;

    public AcpAgent(AcpProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /** Creates a new agent session rooted at the given working directory and returns its ACP session id. */
    public String createSession(String cwd) {
        NewSessionResult result = client()
                .call("session/new", new NewSessionParams(cwd, List.of()), NewSessionResult.class)
                .join();
        return result.sessionId();
    }

    /** Sends a text prompt and blocks until the agent finishes the turn, returning the assembled reply. */
    public PromptTurnResult prompt(String sessionId, String text) {
        TurnAccumulator accumulator = new TurnAccumulator();
        turns.put(sessionId, accumulator);
        try {
            PromptParams params = new PromptParams(sessionId, List.of(ContentBlock.text(text)));
            PromptResult result = client().call("session/prompt", params, PromptResult.class).join();
            return new PromptTurnResult(accumulator.message(), result.stopReason(), accumulator.thought(), accumulator.toolCalls());
        } finally {
            turns.remove(sessionId);
        }
    }

    /** Requests cancellation of whatever prompt turn is currently in flight for this session. */
    public void cancel(String sessionId) {
        client().notify("session/cancel", Map.of("sessionId", sessionId));
    }

    private AcpClient client() {
        AcpClient current = client;
        if (current != null && current.isAlive()) {
            return current;
        }
        synchronized (this) {
            if (client == null || !client.isAlive()) {
                client = start();
            }
            return client;
        }
    }

    /** Whether the underlying agent process is currently running. */
    public boolean isAlive() {
        AcpClient current = client;
        return current != null && current.isAlive();
    }

    /** Starts the agent process if it isn't already running. */
    public void startProcess() {
        client();
    }

    /** Kills the agent process, if running. The next call needing it will start a fresh one. */
    public synchronized void stopProcess() {
        AcpClient current = client;
        if (current != null) {
            current.close();
        }
        client = null;
    }

    private AcpClient start() {
        Map<String, AcpClient.RequestHandler> requestHandlers = Map.of(
                "session/request_permission", this::handlePermissionRequest,
                "fs/read_text_file", params -> readTextFile(objectMapper.treeToValue(params, ReadTextFileParams.class)),
                "fs/write_text_file", params -> writeTextFile(objectMapper.treeToValue(params, WriteTextFileParams.class)));

        Map<String, AcpClient.NotificationHandler> notificationHandlers = Map.of(
                "session/update", this::handleSessionUpdate);

        AcpClient newClient = new AcpClient(
                properties.processCommand(),
                Path.of(properties.workingDirectory()),
                requestHandlers,
                notificationHandlers,
                objectMapper);

        InitializeParams initParams = new InitializeParams(
                PROTOCOL_VERSION,
                new InitializeParams.ClientCapabilities(new InitializeParams.FsCapability(true, true), false),
                new InitializeParams.ClientInfo("multirepo-backend", "0.1.0"));
        newClient.call("initialize", initParams, InitializeResult.class).join();
        return newClient;
    }

    private RequestPermissionResult handlePermissionRequest(tools.jackson.databind.JsonNode paramsNode) {
        RequestPermissionParams params = objectMapper.treeToValue(paramsNode, RequestPermissionParams.class);
        if (!properties.autoApprovePermissions()) {
            log.warn("ACP permission request denied (auto-approve disabled), session={}", params.sessionId());
            return RequestPermissionResult.cancelled();
        }
        RequestPermissionResult result = params.options().stream()
                .filter(o -> "allow_once".equals(o.kind()))
                .findFirst()
                .or(() -> params.options().stream().filter(o -> o.kind() != null && o.kind().startsWith("allow")).findFirst())
                .map(o -> RequestPermissionResult.selected(o.optionId()))
                .orElseGet(RequestPermissionResult::cancelled);
        log.info("ACP permission request auto-{}, session={}, options={}",
                result.outcome().optionId() != null ? "approved(" + result.outcome().optionId() + ")" : "cancelled",
                params.sessionId(), params.options());
        return result;
    }

    private ReadTextFileResult readTextFile(ReadTextFileParams params) {
        try {
            List<String> lines = Files.readAllLines(Path.of(params.path()));
            int start = params.line() != null ? Math.max(params.line() - 1, 0) : 0;
            if (start >= lines.size()) {
                return new ReadTextFileResult("");
            }
            int end = params.limit() != null ? Math.min(start + params.limit(), lines.size()) : lines.size();
            return new ReadTextFileResult(String.join("\n", lines.subList(start, end)));
        } catch (IOException e) {
            throw new AcpException("Failed to read file " + params.path(), e);
        }
    }

    private WriteTextFileResult writeTextFile(WriteTextFileParams params) {
        try {
            Path path = Path.of(params.path());
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, params.content());
            return new WriteTextFileResult();
        } catch (IOException e) {
            throw new AcpException("Failed to write file " + params.path(), e);
        }
    }

    private void handleSessionUpdate(tools.jackson.databind.JsonNode paramsNode) {
        SessionUpdateNotification notification = objectMapper.treeToValue(paramsNode, SessionUpdateNotification.class);
        SessionUpdate update = notification.update();
        TurnAccumulator accumulator = turns.get(notification.sessionId());
        if (update instanceof SessionUpdate.AgentMessageChunk chunk) {
            if (accumulator != null && chunk.content() != null) {
                accumulator.appendMessage(chunk.content().text());
            }
        } else if (update instanceof SessionUpdate.AgentThoughtChunk chunk) {
            if (accumulator != null && chunk.content() != null) {
                accumulator.appendThought(chunk.content().text());
            }
        } else if (update instanceof SessionUpdate.ToolCall toolCall) {
            log.info("ACP tool call [{}]: {} ({})", toolCall.status(), toolCall.title(), toolCall.kind());
            if (accumulator != null) {
                accumulator.addToolCall(new ToolCallEvent(
                        toolCall.toolCallId(), toolCall.title(), toolCall.kind(), toolCall.status()));
            }
        } else if (update instanceof SessionUpdate.ToolCallUpdate toolCallUpdate) {
            log.info("ACP tool call update: {} -> {}", toolCallUpdate.toolCallId(), toolCallUpdate.status());
            if (accumulator != null) {
                accumulator.updateToolCallStatus(toolCallUpdate.toolCallId(), toolCallUpdate.status());
            }
        }
        // user_message_chunk / plan / unknown kinds are not needed by the chat UI.
    }

    @PreDestroy
    public void shutdown() {
        stopProcess();
    }

    private static final class TurnAccumulator {
        private final StringBuilder message = new StringBuilder();
        private final StringBuilder thought = new StringBuilder();
        private final List<ToolCallEvent> toolCalls = new java.util.ArrayList<>();

        synchronized void appendMessage(String text) {
            if (text != null) {
                message.append(text);
            }
        }

        synchronized void appendThought(String text) {
            if (text != null) {
                thought.append(text);
            }
        }

        synchronized void addToolCall(ToolCallEvent event) {
            toolCalls.add(event);
        }

        synchronized void updateToolCallStatus(String toolCallId, String status) {
            for (int i = 0; i < toolCalls.size(); i++) {
                ToolCallEvent existing = toolCalls.get(i);
                if (existing.toolCallId().equals(toolCallId)) {
                    toolCalls.set(i, new ToolCallEvent(existing.toolCallId(), existing.title(), existing.kind(), status));
                    return;
                }
            }
        }

        synchronized String message() {
            return message.toString();
        }

        synchronized String thought() {
            return thought.toString();
        }

        synchronized List<ToolCallEvent> toolCalls() {
            return List.copyOf(toolCalls);
        }
    }
}
