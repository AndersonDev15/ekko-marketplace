package com.ekko.order_service.infrastructure.persistence.adapter.out.product.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductVariantInfo(
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