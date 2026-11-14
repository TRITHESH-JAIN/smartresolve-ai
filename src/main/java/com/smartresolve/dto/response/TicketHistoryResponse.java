package com.smartresolve.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record TicketHistoryResponse(
        UUID id,
        UUID ticketId,
        String action,
        String fieldName,
        String oldValue,
        String newValue,
        UUID performedById,
        String performedByName,
        String remarks,
        LocalDateTime createdAt
) {
}
