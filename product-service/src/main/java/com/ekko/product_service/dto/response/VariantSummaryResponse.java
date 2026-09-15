package com.ekko.product_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Variant summary response for seller variant operations")
public record VariantSummaryResponse(
        @Schema(description = "Variant ID", example = "123e4567-e89b-12d3-a456-426614174006")
        UUID id,

        @Schema(description = "Product ID", example = "123e4567-e89b-12d3-a456-426614174005")
        UUID productId,

        @Schema(description = "Variant SKU", example = "PROD-001-RED-M")
        String sku,

        @Schema(description = "Variant price", example = "29.99")
        BigDecimal price,

        @Schema(description = "Variant discount price", example = "24.99")
        BigDecimal discountPrice,

        @Schema(description = "Currency code", example = "USD")
        String currency,

        @Schema(description = "Whether variant is active", example = "true")
        boolean isActive,

        @Schema(description = "Variant attributes")
        List<VariantAttributeResponse> attributes,

        @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00")
        LocalDateTime createdAt,

        @Schema(description = "Last update timestamp", example = "2024-01-15T10:30:00")
        LocalDateTime updatedAt
) {
}