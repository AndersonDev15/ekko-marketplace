package com.ekko.order_service.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductVariant(
        UUID variantId,
        UUID productId,
        String productName,
        String sku,
        BigDecimal price,
        UUID sellerKeycloakId,
        String storeName,
        String imageUrl,
        Long availableStock
) {
}