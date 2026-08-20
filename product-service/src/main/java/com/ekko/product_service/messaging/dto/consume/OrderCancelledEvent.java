package com.ekko.product_service.messaging.dto.consume;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Consumed from order-service's order.cancelled event. Product-service needs the
 * per-item variantId and quantity to release the reserved stock.
 * previousStatus is informational only (order-service's OrderStatus as string).
 */
public record OrderCancelledEvent(
        UUID orderId,
        String orderNumber,
        UUID customerId,
        String guestEmail,
        String previousStatus,
        boolean refundRequired,
        List<OrderItemCancelled> items,
        LocalDateTime cancelledAt
) {

    public record OrderItemCancelled(
            UUID orderItemId,
            UUID productId,
            UUID variantId,
            Integer quantity
    ) {
    }
}