package com.smartresolve.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ChatRequest(

        UUID conversationId,

        @NotBlank
        @Size(max = 5000)
        String message
) {
}
