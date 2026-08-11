package com.ekko.order_service.domain.model;

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