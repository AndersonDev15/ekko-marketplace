package com.ekko.product_service.messaging.dto.consume;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Consumed from order-service's order.confirmed event. Product-service needs the
 * per-item variantId and quantity to confirm stock of the reserved inventory.
 */
public record OrderConfirmedEvent(
        UUID orderId,
        String orderNumber,
        UUID customerId,
        String guestEmail,
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