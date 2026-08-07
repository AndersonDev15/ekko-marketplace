package com.ekko.product_service.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSummaryResponse(
        UUID id,
        String name,
        String slug,
        BigDecimal price,
        String mainImageUrl,
        String brandName,
        String categoryName,
        boolean hasStock
) {
}