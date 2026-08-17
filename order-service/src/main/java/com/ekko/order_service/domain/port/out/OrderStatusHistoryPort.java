package com.ekko.order_service.domain.port.out;

import com.ekko.order_service.domain.enums.OrderChangeSource;
import com.ekko.order_service.domain.enums.OrderStatus;

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