package com.ekko.order_service.domain.event;

import com.ekko.order_service.domain.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderStatusChangedEvent(
        UUID orderId,
        String orderNumber,
        OrderStatus previousStatus,
        OrderStatus newStatus,
        LocalDateTime changedAt
) {
}