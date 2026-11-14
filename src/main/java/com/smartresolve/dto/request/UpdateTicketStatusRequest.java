package com.smartresolve.dto.request;

import com.smartresolve.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTicketStatusRequest(

        @NotNull(message = "Ticket status is required")
        TicketStatus status,

        @Size(max = 1000, message = "Remarks cannot exceed 1000 characters")
        String remarks
) {
}
