package com.ekko.seller_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SellerAddressRequest(
        @Schema(description = "Street address", example = "Calle 123 #45-67", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String addressLine,

        @Schema(description = "City", example = "Bogotá", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100)
        @NotBlank @Size(max = 100) String city,

        @Schema(description = "State/Department", example = "Cundinamarca", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100)
        @NotBlank @Size(max = 100) String state,

        @Schema(description = "Country", example = "Colombia", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100)
        @NotBlank @Size(max = 100) String country,

        @Schema(description = "Postal code", example = "110111", maxLength = 20)
        @Size(max = 20) String postalCode
) {}
