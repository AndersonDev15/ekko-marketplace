package com.ekko.identity_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to update user credentials (email, first name, last name)")
public record UpdateCredentialsRequest(
        @Schema(description = "User email address", example = "user@example.com")
        @Email String email,

        @Schema(description = "User first name (max 100 characters)", example = "John")
        @Size(max = 100) String firstName,

        @Schema(description = "User last name (max 100 characters)", example = "Doe")
        @Size(max = 100) String lastName
) {
}