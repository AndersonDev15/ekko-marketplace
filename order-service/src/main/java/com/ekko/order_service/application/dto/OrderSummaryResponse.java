package com.ekko.order_service.application.dto;

import com.ekko.order_service.domain.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Order summary for list views")
public record OrderSummaryResponse(
        @Schema(description = "Order UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID id,

        @Schema(description = "Human-readable order number", example = "ORD-20240115-ABC123")
        String orderNumber,

        @Schema(description = "Order status", allowableValues = {"PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"})
        OrderStatus status,

        @Schema(description = "Order total amount", example = "199.99")
        BigDecimal total,

        @Schema(description = "Order creation timestamp")
        LocalDateTime createdAt,

        @Schema(description = "Number of items in order", example = "3")
        Integer itemCount
) {
}