package com.ekko.identity_service.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Error response format")
public record ErrorResponse(
        @Schema(description = "Error code", example = "EMAIL_ALREADY_EXISTS")
        String error,

        @Schema(description = "Human-readable error message", example = "Email already registered: user@example.com")
        String message,

        @Schema(description = "Keycloak user ID if applicable")
        UUID keycloakId,

        @Schema(description = "HTTP status code", example = "409")
        Integer httpStatus
) {
}