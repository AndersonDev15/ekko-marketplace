package com.ekko.order_service.domain.port.out;

import com.ekko.order_service.domain.event.OrderCancelledEvent;
import com.ekko.order_service.domain.event.OrderConfirmedEvent;
import com.ekko.order_service.domain.event.OrderCreatedEvent;
import com.ekko.order_service.domain.event.OrderStatusChangedEvent;

public interface OrderEventPublisherPort {

    void publishOrderCreated(OrderCreatedEvent event);

    void publishOrderCancelled(OrderCancelledEvent event);

    void publishOrderConfirmed(OrderConfirmedEvent event);

    void publishOrderStatusChanged(OrderStatusChangedEvent event);
}