package com.alanquintana.geminiCaller.models;

import jakarta.persistence.*;
import lombok.Data;

/*
 Class Message Definition: Here, we define the messages we send and receive in our conversation.
 Each message has a unique ID, but we receive a chat ID, which corresponds to the chat this message
 belongs to. We also define the property userMessage, a string that stores the message sent by the
 user, and geminiResponse, which is the response that Gemini will generate.
 Additionally, we include a timestamp to record the exact time the message was sent. This allows us
 to store and display chat messages in chronological order.
*/
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

    @Column(length = 1000000)
    private String geminiResponse;
    private Long timestamp;
}
