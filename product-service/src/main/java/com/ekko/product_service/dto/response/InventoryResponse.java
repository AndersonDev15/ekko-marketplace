package com.ekko.product_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Inventory response for variant details")
public record InventoryResponse(
        @Schema(description = "Inventory ID", example = "123e4567-e89b-12d3-a456-426614174008")
        UUID id,

        @Schema(description = "Available stock", example = "100")
        Long stockAvailable,

        @Schema(description = "Reserved stock", example = "10")
        Long stockReserved,

        @Schema(description = "Minimum stock threshold", example = "5")
        Long stockMinimum
) {
}