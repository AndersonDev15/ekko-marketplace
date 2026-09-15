package com.ekko.payment_service.infrastructure.persistence.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create a new vendor account")
public record CreateVendorAccountRequest(
        @Schema(description = "Country code (2-letter ISO)", example = "US", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "country is required")
        @Size(min = 2, max = 2, message = "country must be a 2-letter ISO code")
        String country,

        @Schema(description = "Seller email address", example = "seller@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        String email
) {
}