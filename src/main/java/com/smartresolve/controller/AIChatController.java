package com.smartresolve.controller;

import com.smartresolve.dto.request.ChatRequest;
import com.smartresolve.dto.response.ChatConversationResponse;
import com.smartresolve.dto.response.ChatHistoryResponse;
import com.smartresolve.dto.response.ChatResponse;
import com.smartresolve.service.AIChatService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai/chat")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AIChatController {

    private final AIChatService aiChatService;

    @PostMapping
    public ChatResponse sendMessage(
            @Valid @RequestBody ChatRequest request,
            Authentication authentication
    ) {
        return aiChatService.sendMessage(
                request,
                authentication.getName()
        );
    }

    @GetMapping("/conversations")
    public List<ChatConversationResponse> getConversations(
            Authentication authentication
    ) {
        return aiChatService.getConversations(authentication.getName());
    }

    @GetMapping("/conversations/{conversationId}")
    public ChatHistoryResponse getChatHistory(
            @PathVariable UUID conversationId,
            Authentication authentication
    ) {
        return aiChatService.getChatHistory(
                conversationId,
                authentication.getName()
        );
    }
}
