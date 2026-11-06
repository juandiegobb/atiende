package com.devsenior.atiende.chat;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.atiende.chat.dto.ChatRequest;
import com.devsenior.atiende.chat.dto.ChatResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatbotService chatbotService;

    public ChatController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatbotService.conversar(request));
    }
}

