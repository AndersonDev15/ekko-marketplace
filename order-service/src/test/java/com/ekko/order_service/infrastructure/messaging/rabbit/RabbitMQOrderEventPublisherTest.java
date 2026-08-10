package com.ekko.order_service.infrastructure.messaging.rabbit;

import com.ekko.order_service.domain.model.OrderCreatedEvent;
import com.ekko.order_service.domain.model.OrderStatus;
import com.ekko.order_service.infrastructure.config.RabbitMQConfig;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitMQOrderEventPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final RabbitMQOrderEventPublisher publisher =
            new RabbitMQOrderEventPublisher(rabbitTemplate);

    @Test
    void routesOrderCreatedEventToConfiguredExchangeAndKey() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                "EKK-20250809-AB12",
                "customer@example.com",
                new BigDecimal("199.00"),
                OrderStatus.CONFIRMED,
                LocalDateTime.of(2025, 8, 9, 12, 0));

        publisher.publishOrderCreated(event);

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                event);
    }
}