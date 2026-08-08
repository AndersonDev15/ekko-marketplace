package com.ekko.product_service.dto.request;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductFiltersRequest(
        UUID categoryId,
        UUID brandId,
        UUID sellerId,
        @PositiveOrZero BigDecimal minPrice,
        @PositiveOrZero BigDecimal maxPrice,
        String query
) {
}