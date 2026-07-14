package com.example.demo.chat;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import com.example.demo.chat.dto.AgentLogTurnResponse;
import com.example.demo.chat.dto.LogChunkResponse;
import com.example.demo.chat.dto.SendMessageRequest;

/**
 * Alternative delivery mode for an agent turn: instead of the chat path's held-open chunked
 * POST, this persists progress and hands it out via cursor-based long polling, so a turn
 * survives the caller disconnecting and reconnecting later.
 */
@RestController
@RequestMapping("/api/agent")
public class AgentLogController {

    private final AgentLogService agentLogService;

    public AgentLogController(AgentLogService agentLogService) {
        this.agentLogService = agentLogService;
    }

    @PostMapping("/sessions/{id}/log-turns")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AgentLogTurnResponse start(@PathVariable Long id, @RequestBody SendMessageRequest request) {
        return agentLogService.startTurn(id, request.text());
    }

    @GetMapping("/sessions/{id}/log-turns")
    public List<AgentLogTurnResponse> listTurns(@PathVariable Long id) {
        return agentLogService.listTurns(id);
    }

    @GetMapping("/log-turns/{turnId}")
    public DeferredResult<LogChunkResponse> poll(
            @PathVariable Long turnId,
            @RequestParam(defaultValue = "0") long since,
            @RequestParam(defaultValue = "25000") long timeoutMs) {
        return agentLogService.poll(turnId, since, timeoutMs);
    }

    @GetMapping("/log-turns/{turnId}/status")
    public AgentLogTurnResponse status(@PathVariable Long turnId) {
        return agentLogService.status(turnId);
    }
}
