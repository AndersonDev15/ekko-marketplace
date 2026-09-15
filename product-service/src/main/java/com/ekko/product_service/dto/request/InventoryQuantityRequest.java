package com.ekko.product_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Inventory quantity request for internal operations")
public record InventoryQuantityRequest(
        @Schema(description = "Variant ID", example = "123e4567-e89b-12d3-a456-426614174003", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID variantId,

        @Schema(description = "Quantity", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
        long quantity
) {
}