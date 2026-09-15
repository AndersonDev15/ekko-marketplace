package com.ekko.product_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Product filters for catalog search")
public record ProductFiltersRequest(
        @Schema(description = "Filter by category ID", example = "123e4567-e89b-12d3-a456-426614174001")
        UUID categoryId,

        @Schema(description = "Filter by brand ID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID brandId,

        @Schema(description = "Filter by seller ID", example = "123e4567-e89b-12d3-a456-426614174004")
        UUID sellerId,

        @PositiveOrZero
        @Schema(description = "Minimum price filter", example = "10.00")
        BigDecimal minPrice,

        @PositiveOrZero
        @Schema(description = "Maximum price filter", example = "100.00")
        BigDecimal maxPrice,

        @Schema(description = "Search query text", example = "smartphone")
        String query
) {
}