package com.ekko.order_service.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        String orderNumber,
        String customerEmail,
        BigDecimal total,
        OrderStatus status,
        LocalDateTime createdAt
) {
}