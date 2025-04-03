package com.alanquintana.geminiCaller.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.alanquintana.geminiCaller.services.GeminiService;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/*
* Controller for handling chat requests. This controller manages chat sessions,
* message exchanges and chat navigation.
*/

@Controller
public class ChatController {
    private final GeminiService geminiService;

    //Constructor for the ChatController setting our geminiService.
    public ChatController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    /*
    *
    *
    * This method handles the request for the home page of the application
    *
    * @param Optional new chat, if true, starts a new chat session.
    * @param model the Spring Model object used to pass attributes to the view.
    * @return the name of the view template to render.
    *
    * This method:
    * 1. Starts a new session of the chat if the condition 'newChat' is true.
    * 2. Adds all available chats in our database to the model for sidebar navigation.
    * 3. Adds the current chat ID to the model.
    * 4. Retrieves and adds the chat history of the current chat.
    * 5. Returns the index view, where the chat interface is displayed.
    *
    */


    @GetMapping("/")
    public String index(@RequestParam(required = false) Boolean newChat, Model model) {
        if (Boolean.TRUE.equals(newChat)) {
            geminiService.startNewChat();
        }

        model.addAttribute("allChats", geminiService.getAllChats());
        model.addAttribute("currentChatId", geminiService.getCurrentChatId());
        model.addAttribute("history", geminiService.getHistory());

        return "index";
    }

    /*
    * This controller handle the request to view a specific chat with the parameter chatId
    *
    *   @param id of the chat that is going to be displayed.
    *   @param model the Spring Model object used to pass attributes to the view.
    *   @return string index
    *
    *   This method:
    *   1. Sets the current chat using the method setCurrentChat with the received parameter chatId, using our geminiService
    *   2. Adds all chats in our database to the model for sidebar navigation.
    *   3. Adds the current chat to the model, this chat is the one selected and the one that's going to be displayed
    *   4. Retrieves the history of the chat.
    *   5. return "index" view, where the chat interface is displayed.
    *
    */
    @GetMapping("/chat/{chatId}")
    public String viewChat(@PathVariable Long chatId, Model model) {
        geminiService.setCurrentChat(chatId);

        model.addAttribute("allChats", geminiService.getAllChats());
        model.addAttribute("currentChatId", chatId);
        model.addAttribute("history", geminiService.getHistory());

        return "index";
    }

    /*
    *
    * Handles sending a message to the chat and redirecting to the updated chat view
    *
    * @param message, message sent by the user to the gemini API
    * @param chatId, this id is optional in case the conversation hasn't been created yet.
    * @return, we return a redirection of the view with the id of the current chat.
    *
    * This method:
    * 1. checks if chatId is different from null, if it is, then we set the current chat id.
    * 2. We call our function chatWithGemini, and we send the message as a parameter.
    * 3. We return a redirection of the actual chat to update the view and display the message.
    *
    */

    @PostMapping("/chat")
    public String chat( @RequestParam String message, @RequestParam(required = false) Long chatId) {
        if (chatId != null) {
            geminiService.setCurrentChat(chatId);
        }

        String response = geminiService.chatWithGemini(message);

        return "redirect:/chat/" + geminiService.getCurrentChatId();
    }
}
