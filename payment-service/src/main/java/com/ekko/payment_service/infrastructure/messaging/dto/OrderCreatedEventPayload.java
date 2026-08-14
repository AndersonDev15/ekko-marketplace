package com.ekko.payment_service.infrastructure.messaging.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEventPayload(
        UUID orderId,
        String orderNumber,
        UUID customerId,
        String customerEmail,
        BigDecimal total,
        String status,
        LocalDateTime createdAt,
        List<OrderItemPayload> items
) {

    public record OrderItemPayload(
            UUID variantId,
            UUID productId,
            Integer quantity,
            UUID sellerKeycloakId,
            BigDecimal priceSnapshot,
            BigDecimal subtotal
    ) {
    }
}