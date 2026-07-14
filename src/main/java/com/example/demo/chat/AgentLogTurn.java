package com.example.demo.chat;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A single agent turn whose progress is persisted (rather than pushed) so any client can catch up on it later via polling. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class AgentLogTurn {

    public enum Status { RUNNING, DONE, ERROR }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Lob
    @Column(nullable = false)
    private String prompt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private Instant createdAt;

    public AgentLogTurn(ChatSession session, String prompt) {
        this.session = session;
        this.prompt = prompt;
        this.status = Status.RUNNING;
        this.createdAt = Instant.now();
    }
}
