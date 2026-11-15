package com.smartresolve.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record RateSupportTeamRequest(

        @Min(1)
        @Max(5)
        int rating,

        @Size(max = 2000)
        String feedback
) {
}
