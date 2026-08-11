package com.ekko.order_service.infrastructure.web.dto;

import com.ekko.order_service.domain.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull OrderStatus newStatus
) {
}