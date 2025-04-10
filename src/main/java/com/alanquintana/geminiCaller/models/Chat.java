package com.alanquintana.geminiCaller.models;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;


/*
 Class Chat Definition: This class defines our stored conversations with the LLM model Gemini.
 We store a timestamp for the creation time of each chat, an autogenerated ID that serves as the
 primary key, and the first message of the conversation. Additionally, we establish a one-to-many
 relationship with the Message class, allowing each chat to contain multiple messages.
*/
@Entity
@Data
public class Chat {
    @Id
    @GeneratedValue
    private Long id;

    private Long createdAt;

    private String firstMessage;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messages = new ArrayList<>();
}
