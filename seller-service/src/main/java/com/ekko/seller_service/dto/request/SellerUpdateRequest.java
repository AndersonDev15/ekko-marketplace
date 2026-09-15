package com.ekko.seller_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SellerUpdateRequest(
        @Schema(description = "Store name", example = "My Store", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 255)
        @NotBlank(message = "El nombre de la tienda es obligatorio")
        @Size(max = 255)
        String storeName,

        @Schema(description = "Phone number", example = "+573001234567", maxLength = 20)
        @Size(max = 20)
        String phone,

        @Schema(description = "Store description", example = "Best store in town")
        String description
) {}
