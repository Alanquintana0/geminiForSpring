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

/*
* Service class responsible for handling chat interactions with the gemini API.
* Service for managing chat sessions, stores messages and communicate with the Gemini API.
*/
@Service
public class GeminiService {

    /*
    * We define and initialize objects that will help us perform our operations and exchange with gemini.
    *
    * Logger logger: We define a logger to display or debug errors.
    * MAX_CONTEXT_MESSAGES: Define the number of messages that we will have in our context.
    *
    * WebClient: WebClient instance for making API calls to Gemini
    *
    * Repositories:
    *   chat: Instance for chatRepository
    *   message: Instance for messageRepository
    */
    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    private static final int MAX_CONTEXT_MESSAGES = 10;

    private final WebClient webClient;
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;

    //We define and get our API Key via environment variables.
    @Value("${gemini.api.key}")
    private String apiKey;


    //Definition for our currentChatId.
    private Long currentChatId;

    /*
    * Constructor four our service.
    *
    * @param webClient: we define and build our url to the geminiApi.
    * @param chatRepository: Repository for managing chat sessions.
    * @param messageRepository: Repository for managing messages from chat sessions.
    */
    public GeminiService(WebClient.Builder webClientBuilder, ChatRepository chatRepository, MessageRepository messageRepository) {
        this.webClient = webClientBuilder.baseUrl("https://generativelanguage.googleapis.com/v1beta").build();
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;

    }


    /*
    * Validates the existence of an apiKey.
    * Method executed after bean is constructed.
    *
    * This method:
    * 1. check if apiKey is null or if it has an empty value, if condition is true, we throw an error.
    * 2. Our logger confirms the api set.
    */
    @PostConstruct
    private void validateApiKey() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("Gemini API key not set");
        }
        logger.info("Gemini API key set");
    }


    //Retrieves the id of the current chat session.
    public Long getCurrentChatId() {
        return currentChatId;
    }

    /*
    * Method that checks if we trie to set an existing chat, if not we throw a warn via logger.
    *
    * @param chatId, id of the current chat session.
    *
    * This method:
    *  1. if a chat exists with the given id, then we switch to the chat with this id.
    *  2. if the condition is not true, we warn that we attempted to switch to a non-existing chat session.
    */
    public void setCurrentChat(Long chatId) {
        if (chatRepository.existsById(chatId)) {
            currentChatId = chatId;
            logger.info("Switched to chat with id: {}", currentChatId);
        } else {
            logger.warn("Attempted to switch to non-existent chat: {}", chatId);
        }
    }

    /*
    * This method calls the chatRepository and returns an Iterable with our chats sorted by date in descendant order.
    */
    public Iterable<Chat> getAllChats() {
        return chatRepository.findAllByOrderByCreatedAtDesc();
    }

    /*
    * Method that handles gemini message exchange between gemini api and our application.
    *
    * @param userMessage: message provided by the user that has to be sent to the Gemini API.
    *
    * This method:
    *  1. Checks if the message is null or empty, in case it is we return an empty message.
    *  2. Checks if the chatId is null, if it´s true, we create a new instance of Chat and set
    *       the createdAt property with the current time, then we save the chat with the chat repository and
    *       set the current chat id with the id of the new object.
    *  Inside try block.
    *  3. Build the conversation context to provide message history and to make gemini remember previous messages.
    *  4. Formats and escapes the message into a json request.
    *  5. Makes a post petition to the geminiApi.
    *  6. Parses the api response and extract the text from the Json object we got as response.
    *  7. Creates a new instance of message and set to the current chat, and store the message in our database
    *  8. Returns geminiResponse
    */
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

            String escapedPrompt = fullPrompt.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");

            String requestBody = String.format("{\"contents\":[{\"parts\":[{\"text\":\"%s\"}]}]}", escapedPrompt);
            logger.debug("Gemini full prompt send: {}", fullPrompt);

            Mono<String> response = webClient.post()
                    .uri("/models/gemini-2.0-flash:generateContent?key={apiKey}", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
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

    }

    /*
    * This method returns all the messages of a chat session.
    *
    * @Return iterable with messages from a chat session.
    *
    *  1. Checks if currentChatid is null, if true returns empty arrayList.
    *  2. Returns an iterable of all the messages corresponding to that chat Id.
    */
    public Iterable<Message> getHistory(){
        if(currentChatId == null) {
            return new ArrayList<>();
        }

        return messageRepository.findByChatId(currentChatId);
    }

    /*
    * This method sets the id to null, when a message is sent with the id set to null, we
    * will create a new chat session.
    */
    public void startNewChat() {
        currentChatId = null;
        logger.info("started a new chat session");
    }

    /*
    * Builds context in order to make gemini remember the previous messages in a chat session.
    *
    * @return A formated string containing the last messages set in MAX_CONTEXT_MESSAGES
    *
    * This method:
    * 1. Checks if a chat session exists, if true, returns an empty string to provide no context to the new session.
    * 2. Create a list with the latest messages retrieving them from our database with a limit of 10 messages or the ones set in MAX_CONTENT_MESSAGES.
    * 3. Reverse order to make older messages appear first in context.
    * 4. Formats the messages into a structured conversation with tags for gemini messages and user messages.
    * 5. Return the formated conversation history as a string.
    */
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

    /*
    * Extract the text from the Json response returned by the Gemini API
    *
    * @param jsonResponse, the JSON response received from the Gemini API.
    * @return String with the response from the api or an error if the extraction fails.
    *
    * This method:
    *  1.Parses the Json Response with an instance of ObjectMapper from the Jackson dependency.
    *  2.Navigates the Json Structure in the next order:
    *       - Accesses the 'candidates' array.
    *       - Retrieves the first candidate.
    *       - Navigates to content.parts where the response text is stored.
    *  3. Extracts the text from each text field within parts and appends it to a StringBuilder.
    *  4. Returns the text built or an error in case the extraction fails.
    */
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
}
