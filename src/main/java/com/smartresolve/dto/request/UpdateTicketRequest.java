package com.smartresolve.dto.request;


import com.smartresolve.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateTicketRequest(

        @NotBlank(message = "Ticket title is required")
        @Size(max = 200)
        String title,

        @NotBlank(message = "Ticket description is required")
        @Size(max = 5000)
        String description,

        UUID categoryId,

        TicketPriority priority
) {
}