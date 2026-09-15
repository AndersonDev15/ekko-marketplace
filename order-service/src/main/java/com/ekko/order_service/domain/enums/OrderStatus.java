package com.ekko.order_service.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Order status", allowableValues = {"PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"})
public enum OrderStatus {
    @Schema(description = "Order created, awaiting payment confirmation")
    PENDING,
    @Schema(description = "Payment confirmed, order being processed")
    CONFIRMED,
    @Schema(description = "Order shipped to customer")
    SHIPPED,
    @Schema(description = "Order delivered to customer")
    DELIVERED,
    @Schema(description = "Order cancelled")
    CANCELLED
}