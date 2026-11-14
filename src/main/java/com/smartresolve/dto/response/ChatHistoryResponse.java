package com.smartresolve.dto.response;

import java.util.List;
import java.util.UUID;

public record ChatHistoryResponse(

        UUID conversationId,

        List<ChatMessageResponse> messages
) {
}
