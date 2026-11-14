package com.smartresolve.controller;

import com.smartresolve.dto.request.ModifyAIRecommendationRequest;
import com.smartresolve.dto.request.ReviewAIRecommendationRequest;
import com.smartresolve.dto.response.AIAnalysisResponse;
import com.smartresolve.service.AIRecommendationReviewService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai/recommendations")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AIRecommendationReviewController {

    private final AIRecommendationReviewService reviewService;

    @PostMapping("/{analysisId}/accept")
    public AIAnalysisResponse acceptRecommendation(
            @PathVariable UUID analysisId,
            @Valid @RequestBody(required = false)
            ReviewAIRecommendationRequest request,
            Authentication authentication
    ) {
        return reviewService.acceptRecommendation(
                analysisId, request, authentication.getName());
    }

    @PutMapping("/{analysisId}/modify")
    public AIAnalysisResponse modifyRecommendation(
            @PathVariable UUID analysisId,
            @Valid @RequestBody ModifyAIRecommendationRequest request,
            Authentication authentication
    ) {
        return reviewService.modifyRecommendation(
                analysisId, request, authentication.getName());
    }

    @PostMapping("/{analysisId}/reject")
    public AIAnalysisResponse rejectRecommendation(
            @PathVariable UUID analysisId,
            @Valid @RequestBody(required = false)
            ReviewAIRecommendationRequest request,
            Authentication authentication
    ) {
        return reviewService.rejectRecommendation(
                analysisId, request, authentication.getName());
    }
}
