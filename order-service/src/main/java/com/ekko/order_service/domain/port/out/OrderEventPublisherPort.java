package com.ekko.order_service.domain.port.out;

import com.ekko.order_service.domain.model.OrderCreatedEvent;

public interface OrderEventPublisherPort {

    void publishOrderCreated(OrderCreatedEvent event);
}