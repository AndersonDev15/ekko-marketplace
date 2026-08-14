package com.ekko.payment_service.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVendorAccountRequest(
        @NotBlank(message = "country is required")
        @Size(min = 2, max = 2, message = "country must be a 2-letter ISO code")
        String country,

        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        String email
) {
}