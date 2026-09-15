package com.ekko.product_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Product variant response for detailed views")
public record ProductVariantResponse(
        @Schema(description = "Variant ID", example = "123e4567-e89b-12d3-a456-426614174006")
        UUID id,

        @Schema(description = "Variant SKU", example = "PROD-001-RED-M")
        String sku,

        @Schema(description = "Variant price", example = "29.99")
        BigDecimal price,

        @Schema(description = "Variant discount price", example = "24.99")
        BigDecimal discountPrice,

        @Schema(description = "Currency code", example = "USD")
        String currency,

        @Schema(description = "Whether variant is active", example = "true")
        Boolean isActive,

        @Schema(description = "Inventory information")
        InventoryResponse inventory,

        @Schema(description = "Variant attributes")
        List<ProductVariantAttributeResponse> attributes
) {
}