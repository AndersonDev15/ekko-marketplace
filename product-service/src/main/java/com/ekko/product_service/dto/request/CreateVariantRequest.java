package com.ekko.product_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Request to create a new product variant")
public record CreateVariantRequest(
        @Schema(description = "Variant SKU", example = "PROD-001-RED-M", requiredMode = Schema.RequiredMode.REQUIRED)
        String sku,

        @Schema(description = "Variant price", example = "29.99", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal price,

        @Schema(description = "Variant discount price (optional)", example = "24.99")
        BigDecimal discountPrice,

        @Schema(description = "Currency code", example = "USD", requiredMode = Schema.RequiredMode.REQUIRED)
        String currency,

        @Schema(description = "Variant attributes")
        List<CreateVariantAttributeRequest> attributes
) {
}