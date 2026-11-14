package com.smartresolve.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID ticketId,
        UUID userId,
        String userName,
        String message,
        boolean internalNote,
        LocalDateTime createdAt
) {
}
