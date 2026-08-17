package com.ekko.order_service.domain.event;

import com.ekko.order_service.domain.enums.OrderStatus;

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