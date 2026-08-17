package com.ekko.order_service.infrastructure.persistence.adapter.in.web.dto;

import com.ekko.order_service.domain.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull OrderStatus newStatus
) {
}