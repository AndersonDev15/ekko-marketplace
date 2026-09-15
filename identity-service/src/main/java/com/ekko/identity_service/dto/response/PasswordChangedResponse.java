package com.ekko.identity_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Response after successful password change")
public record PasswordChangedResponse(
        @Schema(description = "Keycloak user ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String keycloakId,

        @Schema(description = "Password change timestamp")
        Instant changedAt
) {
}