package com.smartresolve.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatConversationResponse(

        UUID conversationId,

        String title,

        LocalDateTime createdAt,

        LocalDateTime updatedAt
) {
}
