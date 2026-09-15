package com.ekko.product_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Request to update an existing product variant")
public record UpdateVariantRequest(
        @Schema(description = "Variant SKU", example = "PROD-001-RED-M")
        String sku,

        @Schema(description = "Variant price", example = "29.99")
        BigDecimal price,

        @Schema(description = "Variant discount price (optional)", example = "24.99")
        BigDecimal discountPrice
) {
}