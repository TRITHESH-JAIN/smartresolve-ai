package com.smartresolve.controller;

import com.smartresolve.dto.response.AIAnalysisResponse;
import com.smartresolve.service.AIAnalysisService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AIController {

    private final AIAnalysisService aiAnalysisService;

    @PostMapping("/analyze/{ticketId}")
    public AIAnalysisResponse analyzeTicket(
            @PathVariable UUID ticketId,
            Authentication authentication
    ) {
        return aiAnalysisService.analyzeTicket(
                ticketId,
                authentication.getName()
        );
    }

    @GetMapping("/analyze/{ticketId}")
    public AIAnalysisResponse getLatestAnalysis(
            @PathVariable UUID ticketId,
            Authentication authentication
    ) {
        return aiAnalysisService.getLatestAnalysis(
                ticketId,
                authentication.getName()
        );
    }
}
