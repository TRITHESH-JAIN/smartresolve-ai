package com.smartresolve.dto.ai;

import com.smartresolve.enums.TicketPriority;

import java.util.List;

public record AIAnalysisResult(
        String summary,
        String suggestedCategory,
        TicketPriority suggestedPriority,
        String suggestedTeam,
        List<String> troubleshootingSteps,
        String suggestedResponse
) {
}
