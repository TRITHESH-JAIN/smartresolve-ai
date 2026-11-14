package com.smartresolve.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatResponse(

        UUID conversationId,

        String answer,

        LocalDateTime timestamp
) {
}
