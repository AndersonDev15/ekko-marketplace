package com.ekko.product_service.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductFiltersRequest(
        UUID categoryId,
        UUID brandId,
        UUID sellerId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String query
) {
}