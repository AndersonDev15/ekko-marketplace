package com.ekko.identity_service.dto.response;

import java.time.Instant;

public record PasswordChangedResponse(
        String keycloakId,
        Instant changedAt
) {
}