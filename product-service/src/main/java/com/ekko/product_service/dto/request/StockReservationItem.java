package com.ekko.product_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

@Schema(description = "Stock reservation item for batch reservation")
public record StockReservationItem(
        @NotNull
        @Schema(description = "Variant ID", example = "123e4567-e89b-12d3-a456-426614174003", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID variantId,

        @Positive
        @Schema(description = "Quantity to reserve", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        long quantity
) {
}
