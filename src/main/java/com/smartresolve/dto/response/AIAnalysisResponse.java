package com.smartresolve.dto.response;

import com.smartresolve.enums.AIRecommendationStatus;
import com.smartresolve.enums.TicketPriority;

import java.time.LocalDateTime;
import java.util.UUID;

public record AIAnalysisResponse(
        UUID id,
        UUID ticketId,
        String summary,
        String suggestedCategory,
        TicketPriority suggestedPriority,
        String suggestedTeam,
        String troubleshootingSteps,
        String suggestedResponse,
        AIRecommendationStatus recommendationStatus,
        UUID reviewedById,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt
) {
}
