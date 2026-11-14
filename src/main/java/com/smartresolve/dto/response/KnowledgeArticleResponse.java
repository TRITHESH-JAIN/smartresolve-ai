package com.smartresolve.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record KnowledgeArticleResponse(

        UUID id,

        String title,

        UUID sourceTicketId,

        boolean verified,

        String verifiedBy,

        LocalDateTime createdAt
) {
}
