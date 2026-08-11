package com.ekko.order_service.infrastructure.messaging.rabbit;

import com.ekko.order_service.domain.model.OrderCancelledEvent;
import com.ekko.order_service.domain.model.OrderCreatedEvent;
import com.ekko.order_service.domain.model.OrderStatusChangedEvent;
import com.ekko.order_service.domain.port.out.OrderEventPublisherPort;
import com.ekko.order_service.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RabbitMQOrderEventPublisher implements OrderEventPublisherPort {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishOrderCreated(OrderCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                event
        );
    }

    @Override
    public void publishOrderCancelled(OrderCancelledEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CANCELLED_ROUTING_KEY,
                event
        );
    }

    @Override
    public void publishOrderStatusChanged(OrderStatusChangedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_STATUS_CHANGED_ROUTING_KEY,
                event
        );
    }
}