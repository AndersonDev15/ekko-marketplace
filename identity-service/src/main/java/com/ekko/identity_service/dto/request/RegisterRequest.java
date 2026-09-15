package com.ekko.identity_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to register a new user")
public record RegisterRequest(
        @Schema(description = "User email address", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email String email,

        @Schema(description = "User password (8-128 characters)", example = "SecurePass123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 8, max = 128) String password,

        @Schema(description = "User first name (max 100 characters)", example = "John", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 100) String firstName,

        @Schema(description = "User last name (max 100 characters)", example = "Doe", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 100) String lastName
) {
}