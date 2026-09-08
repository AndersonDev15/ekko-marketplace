package com.ekko.identity_service.dto.response;

import java.time.Instant;

public record RegisterResponse(
        String keycloakId,
        String email,
        String firstName,
        String lastName,
        String role,
        Instant registeredAt
) {
}