package com.smartresolve.dto.response;

import com.smartresolve.enums.TicketPriority;
import com.smartresolve.enums.TicketStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        String title,
        String description,
        String categoryName,
        TicketPriority priority,
        TicketStatus status,
        UUID employeeId,
        String employeeName,
        UUID assignedEngineerId,
        String assignedEngineerName,
        UUID teamId,
        String teamName,
        String resolutionNotes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt,
        LocalDateTime closedAt
) {
}
