package com.carefinder.backend.chat;

import com.carefinder.backend.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @Column(name = "user_message", nullable = false, length = 1000)
    private String userMessage;

    @Column(name = "assistant_message", nullable = false, length = 4000)
    private String assistantMessage;

    @Column(nullable = false, length = 5)
    private String language;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ChatMessage() {
    }

    public ChatMessage(UserAccount user, String userMessage, String assistantMessage, String language) {
        this.user = user;
        this.userMessage = userMessage;
        this.assistantMessage = assistantMessage;
        this.language = language;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public String getAssistantMessage() {
        return assistantMessage;
    }

    public String getLanguage() {
        return language;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
