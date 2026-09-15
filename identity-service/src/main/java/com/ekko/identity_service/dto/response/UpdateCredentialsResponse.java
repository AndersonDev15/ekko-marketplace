package com.ekko.identity_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Response after successful credentials update")
public record UpdateCredentialsResponse(
        @Schema(description = "Keycloak user ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String keycloakId,

        @Schema(description = "User email address", example = "user@example.com")
        String email,

        @Schema(description = "User first name", example = "John")
        String firstName,

        @Schema(description = "User last name", example = "Doe")
        String lastName,

        @Schema(description = "Update timestamp")
        Instant updatedAt
) {
}