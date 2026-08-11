package com.ekko.order_service.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderCancelledEvent(
        UUID orderId,
        String orderNumber,
        UUID customerId,
        String guestEmail,
        OrderStatus previousStatus,
        boolean refundRequired,
        LocalDateTime cancelledAt
) {
}