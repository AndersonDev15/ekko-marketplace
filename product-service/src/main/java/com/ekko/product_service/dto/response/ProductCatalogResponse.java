package com.ekko.product_service.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductCatalogResponse(
        UUID id,
        String name,
        String slug,
        String sellerSlug,
        String description,
        BigDecimal averageRating,
        Integer reviewCount,
        String primaryImageUrl
) {
}