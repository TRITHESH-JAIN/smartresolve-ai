package com.smartresolve.dto.response;

import java.util.UUID;

public record KnowledgeSourceResponse(

        UUID articleId,

        String title,

        UUID sourceTicketId
) {
}
