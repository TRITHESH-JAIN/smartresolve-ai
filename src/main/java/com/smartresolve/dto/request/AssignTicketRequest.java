package com.smartresolve.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AssignTicketRequest(

        @NotNull(message = "Engineer ID is required")
        UUID assignedEngineerId,

        UUID teamId,

        @Size(max = 1000, message = "Remarks cannot exceed 1000 characters")
        String remarks
) {
}
