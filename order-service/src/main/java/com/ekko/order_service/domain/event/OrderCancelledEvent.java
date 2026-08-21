package com.ekko.order_service.domain.event;

import com.ekko.order_service.domain.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderCancelledEvent(
        UUID orderId,
        String orderNumber,
        UUID customerId,
        String customerEmail,
        OrderStatus previousStatus,
        boolean refundRequired,
        List<OrderItemCancelled> items,
        LocalDateTime cancelledAt
) {

    public record OrderItemCancelled(
            UUID orderItemId,
            UUID productId,
            UUID variantId,
            Integer quantity,
            UUID sellerKeycloakId
    ) {
    }
}