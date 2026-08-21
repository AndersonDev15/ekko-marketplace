package com.ekko.notification_service.messaging.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        String orderNumber,
        UUID customerId,
        String customerEmail,
        BigDecimal total,
        OrderStatus status,
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