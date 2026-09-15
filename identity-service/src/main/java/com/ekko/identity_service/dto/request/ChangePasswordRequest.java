package com.ekko.identity_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to change user password")
public record ChangePasswordRequest(
        @Schema(description = "New password (8-128 characters)", example = "NewSecurePass123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 8, max = 128) String newPassword
) {
}