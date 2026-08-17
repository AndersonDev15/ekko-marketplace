package com.ekko.seller_service.messaging.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Consumed from order-service's order.confirmed event.
 * Mirrors the fields published by order-service so metrics can be attributed to
 * each seller that has products in the confirmed order.
 */
public record OrderConfirmedEvent(
        UUID orderId,
        String orderNumber,
        String customerId,
        List<OrderItemConfirmed> items,
        LocalDateTime confirmedAt
) {

    public record OrderItemConfirmed(
            UUID orderItemId,
            UUID productId,
            UUID sellerKeycloakId,
            BigDecimal subtotal
    ) {
    }
}