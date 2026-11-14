package com.smartresolve.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddCommentRequest(

        @NotBlank(message = "Comment cannot be empty")
        @Size(max = 3000, message = "Comment cannot exceed 3000 characters")
        String message,

        boolean internalNote
) {
}
