package com.ekko.order_service.domain.port.out;

import com.ekko.order_service.domain.model.OrderChangeSource;
import com.ekko.order_service.domain.model.OrderStatus;

import java.util.UUID;

public interface OrderStatusHistoryPort {

    void recordStatusChange(
            UUID orderId,
            OrderStatus newStatus,
            OrderChangeSource source,
            UUID changedBy,
            String notes
    );
}