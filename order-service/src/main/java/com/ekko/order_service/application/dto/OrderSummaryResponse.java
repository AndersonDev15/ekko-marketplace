package com.ekko.order_service.application.dto;

import com.ekko.order_service.domain.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID id,
        String orderNumber,
        OrderStatus status,
        BigDecimal total,
        LocalDateTime createdAt,
        Integer itemCount
) {
}