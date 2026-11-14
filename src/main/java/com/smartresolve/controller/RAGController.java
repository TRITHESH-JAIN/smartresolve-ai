package com.smartresolve.controller;

import com.smartresolve.dto.request.ChatRequest;
import com.smartresolve.dto.response.RAGChatResponse;
import com.smartresolve.service.RAGService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/rag")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class RAGController {

    private final RAGService ragService;

    @PostMapping("/chat")
    public RAGChatResponse chat(
            @Valid @RequestBody ChatRequest request,
            Authentication authentication
    ) {
        return ragService.chat(request, authentication.getName());
    }
}
