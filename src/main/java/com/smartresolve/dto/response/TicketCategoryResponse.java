package com.smartresolve.dto.response;

import java.util.UUID;

public record TicketCategoryResponse(
        UUID id,
        String categoryName,
        String description,
        boolean active
) {
}
