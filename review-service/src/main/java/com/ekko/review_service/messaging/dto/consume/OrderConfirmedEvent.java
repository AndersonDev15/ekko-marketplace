package com.ekko.review_service.messaging.dto.consume;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Consumed from order-service's order.confirmed event.
 * The real order-service OrderConfirmedEvent carries, per item, orderItemId, productId,
 * sellerKeycloakId and subtotal; this DTO only declares the fields review-service needs.
 */
public record OrderConfirmedEvent(
        UUID orderId,
        String orderNumber,
        UUID customerId,
        String customerEmail,
        List<OrderItemConfirmed> items,
        LocalDateTime confirmedAt
) {
    public record OrderItemConfirmed(
            UUID orderItemId,
            UUID productId,
            UUID variantId,
            Integer quantity,
            UUID sellerKeycloakId,
            BigDecimal subtotal
    ) {
    }
}