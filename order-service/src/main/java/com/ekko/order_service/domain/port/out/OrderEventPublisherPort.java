package com.ekko.order_service.domain.port.out;

import com.ekko.order_service.domain.model.OrderCancelledEvent;
import com.ekko.order_service.domain.model.OrderCreatedEvent;
import com.ekko.order_service.domain.model.OrderStatusChangedEvent;

public interface OrderEventPublisherPort {

    void publishOrderCreated(OrderCreatedEvent event);

    void publishOrderCancelled(OrderCancelledEvent event);

    void publishOrderStatusChanged(OrderStatusChangedEvent event);
}