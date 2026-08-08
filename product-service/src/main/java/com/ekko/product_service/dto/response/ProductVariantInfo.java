package com.ekko.product_service.dto.response;

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
