package com.smartresolve.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateTicketRequest(

        @NotBlank(message = "Ticket title is required")
        @Size(max = 200, message = "Title cannot exceed 200 characters")
        String title,

        @NotBlank(message = "Ticket description is required")
        @Size(max = 5000, message = "Description cannot exceed 5000 characters")
        String description,

        UUID categoryId
) {
}