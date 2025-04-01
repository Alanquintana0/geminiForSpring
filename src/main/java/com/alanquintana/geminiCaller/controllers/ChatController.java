package com.alanquintana.geminiCaller.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.alanquintana.geminiCaller.services.GeminiService;

@Controller
public class ChatController {
    private final GeminiService geminiService;

    public ChatController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("history", geminiService.getHistory());
        return "index";
    }

    @PostMapping("/chat")
    public String chat(@RequestParam String message, Model model) {
        String response = geminiService.chatWithGemini(message);
        model.addAttribute("history", geminiService.getHistory());
        return "index";
    }
}
