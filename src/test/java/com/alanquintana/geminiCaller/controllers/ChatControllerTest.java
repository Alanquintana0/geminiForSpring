package com.alanquintana.geminiCaller.controllers;

import com.alanquintana.geminiCaller.models.Chat;
import com.alanquintana.geminiCaller.models.Message;
import com.alanquintana.geminiCaller.services.GeminiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
public class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GeminiService geminiService;


    /*
    * This method creates mock instances of our chats and messages objects,
    * when created we test our endpoints in our service.
    *
    * This test:
    *   1. Create mockChats and mockHistory via auxiliary functions
    *   2. We call our methods inside our service to get
    */
    @Test
    public void testIndexWithoutNewChat() throws Exception {
        List<Chat> mockChats = createMockChats();
        List<Message> mockHistory = createMockHistory(mockChats.get(0));

        when(geminiService.getAllChats()).thenReturn(mockChats);
        when(geminiService.getCurrentChatId()).thenReturn(1L);
        when(geminiService.getHistory()).thenReturn(mockHistory);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("allChats", mockChats))
                .andExpect(model().attribute("currentChatId", 1L))
                .andExpect(model().attribute("history", mockHistory));

        verify(geminiService, never()).startNewChat();
        verify(geminiService).getAllChats();
        verify(geminiService).getCurrentChatId();
        verify(geminiService).getHistory();
    }

    @Test
    public void testIndexWithNewChat() throws Exception {
        List<Chat> mockChats = createMockChats();
        List<Message> mockHistory = createMockHistory(mockChats.get(1));

        when(geminiService.getAllChats()).thenReturn(mockChats);
        when(geminiService.getCurrentChatId()).thenReturn(2L);
        when(geminiService.getHistory()).thenReturn(mockHistory);

        mockMvc.perform(get("/").param("newChat", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("allChats", mockChats))
                .andExpect(model().attribute("currentChatId", 2L))
                .andExpect(model().attribute("history", mockHistory));

        verify(geminiService).startNewChat();
        verify(geminiService).getAllChats();
        verify(geminiService).getCurrentChatId();
        verify(geminiService).getHistory();
    }

    @Test
    public void testViewChat() throws Exception {
        Long chatId = 3L;
        List<Chat> mockChats = createMockChats();
        Chat specificChat = new Chat();
        specificChat.setId(chatId);
        specificChat.setFirstMessage("Welcome to chat #3");
        specificChat.setCreatedAt(System.currentTimeMillis());
        mockChats.add(specificChat);

        List<Message> mockHistory = createMockHistory(specificChat);

        when(geminiService.getAllChats()).thenReturn(mockChats);
        when(geminiService.getHistory()).thenReturn(mockHistory);

        mockMvc.perform(get("/chat/{chatId}", chatId))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("allChats", mockChats))
                .andExpect(model().attribute("currentChatId", chatId))
                .andExpect(model().attribute("history", mockHistory));

        verify(geminiService).setCurrentChat(chatId);
        verify(geminiService).getAllChats();
        verify(geminiService).getHistory();
    }

    @Test
    public void testChatWithExistingChatId() throws Exception {
        Long chatId = 4L;
        String userMessage = "Hello, Gemini!";
        String aiResponse = "Hello! How can I assist you today?";

        when(geminiService.getCurrentChatId()).thenReturn(chatId);
        when(geminiService.chatWithGemini(userMessage)).thenReturn(aiResponse);

        mockMvc.perform(post("/chat")
                        .param("message", userMessage)
                        .param("chatId", chatId.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/chat/" + chatId));

        verify(geminiService).setCurrentChat(chatId);
        verify(geminiService).chatWithGemini(userMessage);
        verify(geminiService).getCurrentChatId();
    }

    @Test
    public void testChatWithoutChatId() throws Exception {
        Long newChatId = 5L;
        String userMessage = "Tell me about Spring Boot";
        String aiResponse = "Spring Boot is a Java-based framework...";

        when(geminiService.getCurrentChatId()).thenReturn(newChatId);
        when(geminiService.chatWithGemini(userMessage)).thenReturn(aiResponse);

        mockMvc.perform(post("/chat")
                        .param("message", userMessage))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/chat/" + newChatId));

        verify(geminiService, never()).setCurrentChat(any());
        verify(geminiService).chatWithGemini(userMessage);
        verify(geminiService).getCurrentChatId();
    }

    // Helper methods to create test data

    private List<Chat> createMockChats() {
        List<Chat> chats = new ArrayList<>();

        Chat chat1 = new Chat();
        chat1.setId(1L);
        chat1.setFirstMessage("Hello, I need help with Java");
        chat1.setCreatedAt(System.currentTimeMillis() - 86400000); // 1 day ago

        Chat chat2 = new Chat();
        chat2.setId(2L);
        chat2.setFirstMessage("How do I use Spring Boot?");
        chat2.setCreatedAt(System.currentTimeMillis() - 3600000); // 1 hour ago

        chats.add(chat1);
        chats.add(chat2);

        return chats;
    }

    private List<Message> createMockHistory(Chat chat) {
        List<Message> messages = new ArrayList<>();

        Message message1 = new Message();
        message1.setId(1L);
        message1.setChat(chat);
        message1.setUserMessage(chat.getFirstMessage());
        message1.setGeminiResponse("I'm here to help with your questions about " +
                (chat.getFirstMessage().contains("Java") ? "Java" : "Spring Boot"));
        message1.setTimestamp(chat.getCreatedAt());

        Message message2 = new Message();
        message2.setId(2L);
        message2.setChat(chat);
        message2.setUserMessage("Can you explain more?");
        message2.setGeminiResponse("Certainly! " +
                (chat.getFirstMessage().contains("Java") ?
                        "Java is an object-oriented programming language..." :
                        "Spring Boot is a framework that simplifies Spring application development..."));
        message2.setTimestamp(chat.getCreatedAt() + 60000); // 1 minute after first message

        messages.add(message1);
        messages.add(message2);

        return messages;
    }
}