package com.smartresolve.dto.response;

import com.smartresolve.enums.Role;

import java.util.UUID;

public record AuthResponse(
        String token,
        String tokenType,
        UUID userId,
        String fullName,
        String email,
        Role role
) {
}
