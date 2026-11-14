package com.smartresolve.dto.request;

import jakarta.validation.constraints.Size;

public record ReviewAIRecommendationRequest(

        @Size(max = 1000)
        String remarks
) {
}
