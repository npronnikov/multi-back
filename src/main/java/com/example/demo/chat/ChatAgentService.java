package com.example.demo.chat;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.acp.AcpAgent;
import com.example.demo.acp.AcpProperties;
import com.example.demo.acp.PromptTurnResult;
import com.example.demo.chat.dto.ChatMessageResponse;
import com.example.demo.chat.dto.ChatSessionResponse;
import com.example.demo.chat.dto.SendMessageResult;

@Service
public class ChatAgentService {

    private final AcpAgent acpAgent;
    private final AcpProperties acpProperties;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;

    public ChatAgentService(AcpAgent acpAgent,
                             AcpProperties acpProperties,
                             ChatSessionRepository sessionRepository,
                             ChatMessageRepository messageRepository) {
        this.acpAgent = acpAgent;
        this.acpProperties = acpProperties;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
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

        PromptTurnResult turnResult = acpAgent.prompt(session.getAcpSessionId(), text);

        ChatMessage assistantMessage = messageRepository.save(
                new ChatMessage(session, ChatRole.ASSISTANT, turnResult.text()));

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
}
