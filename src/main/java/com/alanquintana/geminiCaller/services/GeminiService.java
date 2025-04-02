package com.alanquintana.geminiCaller.services;

import com.alanquintana.geminiCaller.models.Chat;
import com.alanquintana.geminiCaller.models.Message;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.alanquintana.geminiCaller.repositories.ChatRepository;
import org.springframework.beans.factory.annotation.Value;
import com.alanquintana.geminiCaller.repositories.MessageRepository;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    private final WebClient webClient;
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;

    @Value("${gemini.api.key}")
    private String apiKey;


    private Long currentChatId;

    public GeminiService(WebClient.Builder webClientBuilder, ChatRepository chatRepository, MessageRepository messageRepository) {
        this.webClient = webClientBuilder.baseUrl("https://generativelanguage.googleapis.com/v1beta").build();
//        this.webClient = webClient;
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;

    }

    @PostConstruct
    private void validateApiKey() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("Gemini API key not set");
        }
        System.out.println("Gemini API Key: " + apiKey);
        logger.info("Gemini API key set: {}", apiKey);
    }

    public Long getCurrentChatId() {
        return currentChatId;
    }

    public void setCurrentChat(Long chatId) {
        if (chatRepository.existsById(chatId)) {
            currentChatId = chatId;
            logger.info("Switched to chat with id: {}", currentChatId);
        } else {
            logger.warn("Attempted to switch to non-existent chat: {}", chatId);
        }
    }

    public Iterable<Chat> getAllChats() {
        return chatRepository.findAllByOrderByCreatedAtDesc();
    }

//    public Mono<String> chatWithGemini(String userMessage){
//        if(currentChatId == null){
//            Chat chat = new Chat();
//            chat.setCreatedAt(System.currentTimeMillis());
//            chat = chatRepository.save(chat);
//            currentChatId = chat.getId();
//            logger.info("Gemini chat created with id: {}", currentChatId);
//        }
//
//        String requestBody = String.format("{\"contents\":[{\"parts\":[{\"text\":\"%s\"}]}]}", userMessage);
//
//        return webClient.post()
//                .uri("/models/gemini-2.0-flash:generateContent?key={apiKey}", apiKey)
//                .bodyValue(requestBody)
//                .retrieve()
//                .bodyToMono(String.class)
//                .doOnNext(geminiResponse -> {
//                    Message message = new Message();
//                    message.setChat(chatRepository.findById(currentChatId).orElseThrow());
//                    message.setUserMessage(userMessage);
//                    message.setGeminiResponse(geminiResponse);
//                    message.setTimestamp(System.currentTimeMillis());
//                    messageRepository.save(message);
//                    logger.info("Gemini message created: {}", message);
//                })
//                .doOnError(error -> logger.error("Gemini message could not be generated", error));
//    }
//
//    public Iterable<Message> getHistory() {
//        if(currentChatId == null){
//            logger.warn("Gemini API key not set");
//            return new ArrayList<>();
//        }
//        return messageRepository.findByChatId(currentChatId);
//    }

    public String chatWithGemini(String userMessage) {
        if(currentChatId == null) {
            Chat chat = new Chat();
            chat.setCreatedAt(System.currentTimeMillis());
            chat = chatRepository.save(chat);
            currentChatId = chat.getId();
        }

        String requestBody = String.format("{\"contents\":[{\"parts\":[{\"text\":\"%s\"}]}]}", userMessage);

        Mono<String> response = webClient.post()
                .uri("/models/gemini-2.0-flash:generateContent?key={apiKey}", apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class);

        String geminiResponse = response.block();

        Message message = new Message();
        message.setChat(chatRepository.findById(currentChatId).orElseThrow());
        message.setUserMessage(userMessage);
        message.setGeminiResponse(geminiResponse);
        message.setTimestamp(System.currentTimeMillis());
        messageRepository.save(message);

        return geminiResponse;

    }

    public Iterable<Message> getHistory(){
        if(currentChatId == null) {
            return new ArrayList<>();
        }

        return messageRepository.findByChatId(currentChatId);
    }

    public void startNewChat() {
        currentChatId = null;
        logger.info("started a new chat session");
    }

//    public String chatWithGemini(String userMessage) {
//        if()
//
//        return null;
//    }
}
