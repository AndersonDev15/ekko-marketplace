package com.ekko.order_service.infrastructure.persistence.adapter.in.web.dto;

import com.ekko.order_service.domain.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to update order status (admin only)")
public record UpdateOrderStatusRequest(
        @Schema(description = "New order status", allowableValues = {"PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"}, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull OrderStatus newStatus
) {
}