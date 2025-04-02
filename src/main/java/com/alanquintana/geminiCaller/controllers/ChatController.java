package com.alanquintana.geminiCaller.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.alanquintana.geminiCaller.services.GeminiService;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ChatController {
    private final GeminiService geminiService;

    public ChatController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @GetMapping("/")
    public String index(@RequestParam(required = false) Boolean newChat, Model model) {
        if (Boolean.TRUE.equals(newChat)) {
            geminiService.startNewChat();
        }

        // Add all chats to the model for the sidebar
        model.addAttribute("allChats", geminiService.getAllChats());
        model.addAttribute("currentChatId", geminiService.getCurrentChatId());
        model.addAttribute("history", geminiService.getHistory());

        return "index";
    }

    @GetMapping("/chat/{chatId}")
    public String viewChat(@PathVariable Long chatId, Model model) {
        // Set the current chat to the requested one
        geminiService.setCurrentChat(chatId);

        // Add all chats to the model for the sidebar
        model.addAttribute("allChats", geminiService.getAllChats());
        model.addAttribute("currentChatId", chatId);
        model.addAttribute("history", geminiService.getHistory());

        return "index";
    }

    @PostMapping("/chat")
    public String chat(
            @RequestParam String message,
            @RequestParam(required = false) Long chatId,
            RedirectAttributes redirectAttributes,
            Model model) {

        // If chatId is provided, set it as the current chat
        if (chatId != null) {
            geminiService.setCurrentChat(chatId);
        }

        // Send the message
        String response = geminiService.chatWithGemini(message);

        return "redirect:/chat/" + geminiService.getCurrentChatId();
    }
}
