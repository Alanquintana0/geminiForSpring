package com.alanquintana.geminiCaller.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    private String userMessage;

    @Column(length = 10000)
    private String geminiResponse;
    private Long timestamp;
}
