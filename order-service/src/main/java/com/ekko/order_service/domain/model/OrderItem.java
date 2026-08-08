package com.ekko.order_service.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItem(
        UUID id,
        UUID variantId,
        UUID productId,
        String productNameSnapshot,
        String variantSnapshot,
        UUID sellerKeycloakId,
        String sellerNameSnapshot,
        BigDecimal priceSnapshot,
        Integer quantity,
        BigDecimal subtotal,
        String imageUrlSnapshot
) {
}