package com.smartresolve.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RAGChatResponse(

        UUID conversationId,

        String answer,

        List<KnowledgeSourceResponse> sources,

        LocalDateTime timestamp
) {
}
