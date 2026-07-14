package com.example.demo.chat;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.acp.AcpAgent;
import com.example.demo.acp.PromptTurnResult;
import com.example.demo.acp.RecoveredSession;
import com.example.demo.acp.TurnListener;
import com.example.demo.acp.dto.ToolCallEvent;
import com.example.demo.acp.dto.ToolCallStatusEvent;
import com.example.demo.chat.dto.AgentLogTurnResponse;
import com.example.demo.chat.dto.LogChunkResponse;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Alternative to {@link ChatAgentService#streamMessage}: instead of pushing the turn over a
 * held-open response, every chunk is appended as an NDJSON line to a file on disk (one file per
 * turn), and handed out to whoever asks via byte-offset long polling. Slower to the first byte
 * than the chat path, but the turn is a durable, resumable server-side artifact - callers can
 * disconnect and reconnect (or have several callers watch the same turn) without losing anything.
 */
@Service
public class AgentLogService {

    private static final Logger log = LoggerFactory.getLogger(AgentLogService.class);
    private static final Path LOG_DIR = Path.of("data", "agent-logs");

    private final AcpAgent acpAgent;
    private final ChatSessionRepository sessionRepository;
    private final AgentLogTurnRepository turnRepository;
    private final ObjectMapper objectMapper;
    private final ExecutorService turnExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<Long, CopyOnWriteArrayList<PendingPoll>> waiters = new ConcurrentHashMap<>();

    public AgentLogService(AcpAgent acpAgent,
                            ChatSessionRepository sessionRepository,
                            AgentLogTurnRepository turnRepository,
                            ObjectMapper objectMapper) {
        this.acpAgent = acpAgent;
        this.sessionRepository = sessionRepository;
        this.turnRepository = turnRepository;
        this.objectMapper = objectMapper;
    }

    public AgentLogTurnResponse startTurn(Long sessionId, String text) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        AgentLogTurn turn = turnRepository.save(new AgentLogTurn(session, text));

        turnExecutor.execute(() -> runTurn(turn.getId(), session, text));
        return AgentLogTurnResponse.from(turn);
    }

    public AgentLogTurnResponse status(Long turnId) {
        return AgentLogTurnResponse.from(requireTurn(turnId));
    }

    public List<AgentLogTurnResponse> listTurns(Long sessionId) {
        return turnRepository.findBySession_IdOrderByCreatedAtDesc(sessionId).stream()
                .map(AgentLogTurnResponse::from)
                .toList();
    }

    /**
     * Long-polls for file content after byte offset {@code since}. Resolves immediately if a
     * complete new line is already on disk; otherwise parks the request (up to {@code timeoutMs})
     * until a new line is written, then resolves with an empty chunk on timeout so the caller
     * just polls again with the same offset.
     */
    public DeferredResult<LogChunkResponse> poll(Long turnId, long since, long timeoutMs) {
        AgentLogTurn turn = requireTurn(turnId);

        LogChunk chunk = readSince(logPath(turnId), since);
        DeferredResult<LogChunkResponse> result =
                new DeferredResult<>(timeoutMs, new LogChunkResponse("", since, turn.getStatus().name()));
        if (!chunk.text().isEmpty() || turn.getStatus() != AgentLogTurn.Status.RUNNING) {
            // Either there's fresh data, or the turn is over and nothing more will ever arrive -
            // resolve immediately instead of parking the caller for the full timeout.
            result.setResult(new LogChunkResponse(chunk.text(), chunk.nextOffset(), turn.getStatus().name()));
            return result;
        }

        PendingPoll pending = new PendingPoll(since, result);
        CopyOnWriteArrayList<PendingPoll> turnWaiters = waiters.computeIfAbsent(turnId, k -> new CopyOnWriteArrayList<>());
        turnWaiters.add(pending);
        Runnable cleanup = () -> turnWaiters.remove(pending);
        result.onCompletion(cleanup);
        result.onTimeout(cleanup);
        return result;
    }

    private void runTurn(Long turnId, ChatSession session, String text) {
        Path path = logPath(turnId);
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            TurnListener listener = new TurnListener() {
                @Override
                public void onThought(String chunk) {
                    writeLine(writer, turnId, "thought", chunk);
                }

                @Override
                public void onMessage(String chunk) {
                    writeLine(writer, turnId, "message", chunk);
                }

                @Override
                public void onToolCall(ToolCallEvent event) {
                    writeLine(writer, turnId, "tool_call", event);
                }

                @Override
                public void onToolCallUpdate(String toolCallId, String status) {
                    writeLine(writer, turnId, "tool_call_update", new ToolCallStatusEvent(toolCallId, status));
                }
            };

            try {
                PromptTurnResult result;
                try {
                    result = acpAgent.prompt(session.getAcpSessionId(), text, listener);
                } catch (RuntimeException e) {
                    if (!AcpAgent.isSessionNotFoundError(e)) {
                        throw e;
                    }
                    // The agent process was restarted since this session was created, so it no
                    // longer recognizes this id - recover it (reload from disk if possible) and
                    // retry once.
                    RecoveredSession recovered = acpAgent.recoverSession(session.getCwd(), session.getAcpSessionId());
                    session.setAcpSessionId(recovered.sessionId());
                    sessionRepository.save(session);
                    result = acpAgent.prompt(recovered.sessionId(), text, listener);
                }
                writeLine(writer, turnId, "done", Map.of("stopReason", result.stopReason()));
                finishTurn(turnId, AgentLogTurn.Status.DONE);
            } catch (Exception e) {
                log.warn("Logged agent turn {} failed", turnId, e);
                writeLine(writer, turnId, "error", Map.of("message", String.valueOf(e.getMessage())));
                finishTurn(turnId, AgentLogTurn.Status.ERROR);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void finishTurn(Long turnId, AgentLogTurn.Status status) {
        AgentLogTurn turn = requireTurn(turnId);
        turn.setStatus(status);
        turnRepository.save(turn);
    }

    private void writeLine(BufferedWriter writer, Long turnId, String type, Object data) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("type", type);
            node.set("data", objectMapper.valueToTree(data));
            writer.write(objectMapper.writeValueAsString(node));
            writer.write("\n");
            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        notifyWaiters(turnId);
    }

    private void notifyWaiters(Long turnId) {
        CopyOnWriteArrayList<PendingPoll> turnWaiters = waiters.get(turnId);
        if (turnWaiters == null) {
            return;
        }
        Path path = logPath(turnId);
        for (PendingPoll pending : turnWaiters) {
            LogChunk chunk = readSince(path, pending.since());
            if (!chunk.text().isEmpty() && turnWaiters.remove(pending)) {
                String status = requireTurn(turnId).getStatus().name();
                pending.result().setResult(new LogChunkResponse(chunk.text(), chunk.nextOffset(), status));
            }
        }
    }

    /** Reads whatever complete lines exist after {@code since}, holding back any trailing partial line. */
    private LogChunk readSince(Path path, long since) {
        try {
            if (!Files.exists(path)) {
                return new LogChunk("", since);
            }
            byte[] all = Files.readAllBytes(path);
            if (since >= all.length) {
                return new LogChunk("", since);
            }
            byte[] tail = Arrays.copyOfRange(all, (int) since, all.length);
            String text = new String(tail, StandardCharsets.UTF_8);
            int lastNewline = text.lastIndexOf('\n');
            if (lastNewline < 0) {
                return new LogChunk("", since);
            }
            String complete = text.substring(0, lastNewline + 1);
            long nextOffset = since + complete.getBytes(StandardCharsets.UTF_8).length;
            return new LogChunk(complete, nextOffset);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path logPath(Long turnId) {
        return LOG_DIR.resolve("turn-" + turnId + ".log");
    }

    private AgentLogTurn requireTurn(Long turnId) {
        return turnRepository.findById(turnId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private record PendingPoll(long since, DeferredResult<LogChunkResponse> result) {
    }

    private record LogChunk(String text, long nextOffset) {
    }
}
