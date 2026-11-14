package com.smartresolve.dto.request;

import com.smartresolve.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ModifyAIRecommendationRequest(

        @NotBlank
        @Size(max = 2000)
        String summary,

        @NotNull
        UUID categoryId,

        @NotNull
        TicketPriority priority,

        @NotNull
        UUID teamId,

        @NotBlank
        @Size(max = 5000)
        String troubleshootingSteps,

        @NotBlank
        @Size(max = 3000)
        String suggestedResponse,

        @Size(max = 1000)
        String reviewRemarks
) {
}
