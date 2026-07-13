package com.example.demo.acp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Generic JSON-RPC 2.0 client for the Agent Client Protocol (ACP), transported as
 * newline-delimited JSON over the stdin/stdout of a spawned agent process.
 * This class knows nothing about ACP method semantics - it only frames messages,
 * correlates requests with responses, and dispatches incoming requests/notifications
 * to caller-supplied handlers.
 */
public class AcpClient implements AutoCloseable {

    /** Handles a JSON-RPC request sent by the agent to the client and returns the result to serialize back. */
    public interface RequestHandler {
        Object handle(JsonNode params);
    }

    /** Handles a JSON-RPC notification sent by the agent to the client. */
    public interface NotificationHandler {
        void handle(JsonNode params);
    }

    private static final Logger log = LoggerFactory.getLogger(AcpClient.class);

    private final ObjectMapper objectMapper;
    private final Process process;
    private final BufferedWriter stdin;
    private final Object writeLock = new Object();
    private final AtomicLong idSequence = new AtomicLong(1);
    private final ConcurrentHashMap<Long, CompletableFuture<JsonNode>> pending = new ConcurrentHashMap<>();
    private final Map<String, RequestHandler> requestHandlers;
    private final Map<String, NotificationHandler> notificationHandlers;
    private final Thread readerThread;
    private final Thread stderrThread;

    public AcpClient(List<String> command,
                      Path workingDirectory,
                      Map<String, RequestHandler> requestHandlers,
                      Map<String, NotificationHandler> notificationHandlers,
                      ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.requestHandlers = requestHandlers;
        this.notificationHandlers = notificationHandlers;
        try {
            this.process = new ProcessBuilder(command)
                    .directory(workingDirectory.toFile())
                    .start();
        } catch (IOException e) {
            throw new AcpException("Failed to start ACP agent process " + command, e);
        }
        this.stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));

        this.readerThread = new Thread(this::readLoop, "acp-client-reader");
        this.readerThread.setDaemon(true);
        this.readerThread.start();

        this.stderrThread = new Thread(this::drainStderr, "acp-client-stderr");
        this.stderrThread.setDaemon(true);
        this.stderrThread.start();
    }

    /** Sends a JSON-RPC request and returns a future completed with the deserialized result. */
    public <T> CompletableFuture<T> call(String method, Object params, Class<T> resultType) {
        long id = idSequence.getAndIncrement();
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pending.put(id, future);

        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put("jsonrpc", "2.0");
        envelope.put("id", id);
        envelope.put("method", method);
        envelope.set("params", objectMapper.valueToTree(params));
        writeLine(envelope);

        return future.thenApply(node -> objectMapper.treeToValue(node, resultType));
    }

    /** Sends a JSON-RPC notification (no response expected). */
    public void notify(String method, Object params) {
        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put("jsonrpc", "2.0");
        envelope.put("method", method);
        envelope.set("params", objectMapper.valueToTree(params));
        writeLine(envelope);
    }

    public boolean isAlive() {
        return process.isAlive();
    }

    private void writeLine(ObjectNode envelope) {
        String line = objectMapper.writeValueAsString(envelope);
        synchronized (writeLock) {
            try {
                stdin.write(line);
                stdin.write("\n");
                stdin.flush();
            } catch (IOException e) {
                throw new AcpException("Failed to write to ACP agent process", e);
            }
        }
    }

    private void readLoop() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                try {
                    dispatch(objectMapper.readTree(line));
                } catch (Exception e) {
                    log.warn("Failed to process ACP message line: {}", line, e);
                }
            }
        } catch (IOException e) {
            log.warn("ACP agent stdout stream closed: {}", e.getMessage());
        } finally {
            AcpException closed = new AcpException("ACP agent process terminated before responding");
            pending.values().forEach(f -> f.completeExceptionally(closed));
            pending.clear();
        }
    }

    private void drainStderr() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("[acp-agent stderr] {}", line);
            }
        } catch (IOException ignored) {
            // stream closes when the process exits
        }
    }

    private void dispatch(JsonNode node) {
        JsonNode idNode = node.get("id");
        JsonNode methodNode = node.get("method");
        String method = (methodNode != null && !methodNode.isNull()) ? methodNode.asString() : null;

        if (method != null && idNode != null) {
            handleIncomingRequest(idNode, method, node.get("params"));
        } else if (method != null) {
            handleNotification(method, node.get("params"));
        } else if (idNode != null) {
            handleResponse(idNode, node);
        } else {
            log.warn("Unrecognized ACP message: {}", node);
        }
    }

    private void handleIncomingRequest(JsonNode idNode, String method, JsonNode params) {
        RequestHandler handler = requestHandlers.get(method);
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", idNode);

        if (handler == null) {
            ObjectNode error = objectMapper.createObjectNode();
            error.put("code", -32601);
            error.put("message", "Method not found: " + method);
            response.set("error", error);
        } else {
            try {
                Object result = handler.handle(params);
                response.set("result", objectMapper.valueToTree(result));
            } catch (Exception e) {
                log.warn("ACP client request handler for {} failed", method, e);
                ObjectNode error = objectMapper.createObjectNode();
                error.put("code", -32603);
                error.put("message", String.valueOf(e.getMessage()));
                response.set("error", error);
            }
        }
        writeLine(response);
    }

    private void handleNotification(String method, JsonNode params) {
        NotificationHandler handler = notificationHandlers.get(method);
        if (handler == null) {
            return;
        }
        try {
            handler.handle(params);
        } catch (Exception e) {
            log.warn("ACP client notification handler for {} failed", method, e);
        }
    }

    private void handleResponse(JsonNode idNode, JsonNode node) {
        CompletableFuture<JsonNode> future = pending.remove(idNode.asLong());
        if (future == null) {
            return;
        }
        if (node.has("error")) {
            JsonNode error = node.get("error");
            String message = error.path("message").asString("");
            String details = error.path("data").path("details").asString(null);
            if (details != null && !details.isBlank()) {
                message = message + " (" + details + ")";
            }
            future.completeExceptionally(new AcpException(
                    "ACP agent error " + error.path("code").asInt() + ": " + message));
        } else {
            future.complete(node.get("result"));
        }
    }

    @Override
    public void close() {
        try {
            stdin.close();
        } catch (IOException ignored) {
            // best effort
        }
        process.destroy();
        readerThread.interrupt();
        stderrThread.interrupt();
    }
}
