package com.alanquintana.geminiCaller.services;

import com.alanquintana.geminiCaller.models.Chat;
import com.alanquintana.geminiCaller.models.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.alanquintana.geminiCaller.repositories.ChatRepository;
import org.springframework.beans.factory.annotation.Value;
import com.alanquintana.geminiCaller.repositories.MessageRepository;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    private static final int MAX_CONTEXT_MESSAGES = 10;

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
        if(userMessage == null || userMessage.trim().isEmpty()) {
            logger.warn("Gemini Message not set");
            return "Message cannot be empty";
        }

        if(currentChatId == null) {
            Chat chat = new Chat();
            chat.setCreatedAt(System.currentTimeMillis());
            chat = chatRepository.save(chat);
            currentChatId = chat.getId();
        }

        try {
            String conversationContext = buildConversationContext();
            String fullPrompt = conversationContext.isEmpty() ? userMessage : conversationContext + "\n\nUser: " + userMessage;

            String escapedPrompt = fullPrompt.replace("\\", "\\\\")  // Escape backslashes first
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");

            String requestBody = String.format("{\"contents\":[{\"parts\":[{\"text\":\"%s\"}]}]}", escapedPrompt);
            logger.debug("Gemini full prompt send: {}", fullPrompt);

            Mono<String> response = webClient.post()
                    .uri("/models/gemini-2.0-flash:generateContent?key={apiKey}", apiKey)
                    .contentType(MediaType.APPLICATION_JSON) // Added Content-Type
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class);

            String responseGemini = response.block();
            String extractedText = extractTextFromGeminiResponse(responseGemini);

            Chat chat = chatRepository.findById(currentChatId)
                    .orElseThrow(() -> new RuntimeException("Current chat not found"));

            Message message = new Message();
            message.setChat(chat);
            message.setUserMessage(userMessage);
            message.setGeminiResponse(extractedText);
            message.setTimestamp(System.currentTimeMillis());
            messageRepository.save(message);

            logger.info("Stored message in chat {}: user message length={}, response length={}",
                    currentChatId, userMessage.length(), responseGemini.length());
            responseGemini = extractTextFromGeminiResponse(responseGemini);

            return responseGemini;

        } catch (WebClientResponseException e) {
            String errorDetails = e.getResponseBodyAsString();
            logger.error("Error communicating with Gemini API: {}", errorDetails);
            try {
                Chat chat = chatRepository.findById(currentChatId)
                        .orElseThrow(() -> new RuntimeException("Current chat not found"));
                Message errorMessage = new Message();
                errorMessage.setChat(chat);
                errorMessage.setUserMessage(userMessage);
                errorMessage.setGeminiResponse("Error: " + errorDetails);
                errorMessage.setTimestamp(System.currentTimeMillis());
                messageRepository.save(errorMessage);
            } catch (Exception dbError) {
                logger.error("Could not save error message to database", dbError);
            }
            return "Error communicating with Gemini API: " + errorDetails;
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            return "Unexpected error: " + e.getMessage();
        }


//        String requestBody = String.format("{\"contents\":[{\"parts\":[{\"text\":\"%s\"}]}]}", userMessage);
//
//        Mono<String> response = webClient.post()
//                .uri("/models/gemini-2.0-flash:generateContent?key={apiKey}", apiKey)
//                .bodyValue(requestBody)
//                .retrieve()
//                .bodyToMono(String.class);
//
//        String geminiResponse = response.block();
//
//        Message message = new Message();
//        message.setChat(chatRepository.findById(currentChatId).orElseThrow());
//        message.setUserMessage(userMessage);
//        message.setGeminiResponse(geminiResponse);
//        message.setTimestamp(System.currentTimeMillis());
//        messageRepository.save(message);
//
//        return geminiResponse;

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

    private String buildConversationContext(){
        if(currentChatId == null) {
            return "";
        }

        List<Message> recentMessages = StreamSupport
                .stream(messageRepository.findByChatIdOrderByTimestampDesc(currentChatId).spliterator(), false)
                .limit(MAX_CONTEXT_MESSAGES)
                .collect(Collectors.toList());

        java.util.Collections.reverse(recentMessages);

        StringBuilder conversationContext = new StringBuilder();

        for(Message msg : recentMessages){
            if(conversationContext.length() > 0) {
                conversationContext.append("\n\n");
            }
            conversationContext.append("User: ").append(msg.getUserMessage());
            conversationContext.append("\nGemini: ").append(msg.getGeminiResponse());
        }

        return conversationContext.toString();
    }

    private String extractTextFromGeminiResponse(String jsonResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResponse);
            JsonNode partsNode = root.path("candidates").get(0).path("content").path("parts");
            StringBuilder textBuilder = new StringBuilder();
            for (JsonNode part : partsNode) {
                textBuilder.append(part.path("text").asText());
            }
            return textBuilder.toString();
        } catch (Exception e) {
            System.err.println("Error extracting text from Gemini response: " + e.getMessage());
            return "Error extracting text";
        }
    }
//    public String chatWithGemini(String userMessage) {
//        if()
//
//        return null;
//    }
}
