package com.example.demo.chat;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String acpSessionId;

    private String cwd;

    private String currentModelId;

    @Lob
    private String availableModelsJson;

    @Column(nullable = false)
    private Instant createdAt;

    public ChatSession(String acpSessionId, String cwd) {
        this.acpSessionId = acpSessionId;
        this.cwd = cwd;
        this.createdAt = Instant.now();
    }
}
