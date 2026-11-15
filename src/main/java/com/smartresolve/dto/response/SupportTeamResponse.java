package com.smartresolve.dto.response;

import java.util.UUID;

public record SupportTeamResponse(
        UUID id,
        String teamName,
        String description,
        boolean active
) {
}
