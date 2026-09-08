package com.ekko.identity_service.dto.response;

import java.time.Instant;

public record UpdateCredentialsResponse(
        String keycloakId,
        String email,
        String firstName,
        String lastName,
        Instant updatedAt
) {
}