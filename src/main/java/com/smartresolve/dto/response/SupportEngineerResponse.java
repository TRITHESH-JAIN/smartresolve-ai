package com.smartresolve.dto.response;

import java.util.UUID;

public record SupportEngineerResponse(
        UUID id,
        String fullName,
        String email,
        UUID teamId,
        String teamName
) {
}
