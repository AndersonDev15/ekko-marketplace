package com.ekko.product_service.messaging;

import com.ekko.product_service.config.RabbitMQConfig;
import com.ekko.product_service.enums.ProductStatus;
import com.ekko.product_service.messaging.dto.publish.InventoryLowStockEvent;
import com.ekko.product_service.messaging.dto.publish.ProductDeactivatedEvent;
import com.ekko.product_service.messaging.dto.publish.ProductPublishedEvent;
import com.ekko.product_service.messaging.dto.publish.ProductRejectedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitMQProductEventPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final RabbitMQProductEventPublisher publisher =
            new RabbitMQProductEventPublisher(rabbitTemplate);

    @Test
    void routesProductPublishedEventToConfiguredExchangeAndKey() {
        ProductPublishedEvent event = new ProductPublishedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "iPhone 16",
                "Phones",
                LocalDateTime.of(2026, 1, 1, 10, 0));

        publisher.publishProductPublished(event);

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.PRODUCT_EXCHANGE,
                RabbitMQConfig.PRODUCT_PUBLISHED_ROUTING_KEY,
                event);
    }

    @Test
    void routesProductRejectedEventToConfiguredExchangeAndKey() {
        ProductRejectedEvent event = new ProductRejectedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "iPhone 16",
                "marca prohibida",
                LocalDateTime.of(2026, 1, 1, 10, 0));

        publisher.publishProductRejected(event);

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.PRODUCT_EXCHANGE,
                RabbitMQConfig.PRODUCT_REJECTED_ROUTING_KEY,
                event);
    }

    @Test
    void routesProductDeactivatedEventToConfiguredExchangeAndKey() {
        ProductDeactivatedEvent event = new ProductDeactivatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ProductStatus.ACTIVE,
                LocalDateTime.of(2026, 1, 1, 10, 0));

        publisher.publishProductDeactivated(event);

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.PRODUCT_EXCHANGE,
                RabbitMQConfig.PRODUCT_DEACTIVATED_ROUTING_KEY,
                event);
    }

    @Test
    void routesInventoryLowStockEventToConfiguredExchangeAndKey() {
        InventoryLowStockEvent event = new InventoryLowStockEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                2L,
                3L,
                LocalDateTime.of(2026, 1, 1, 10, 0));

        publisher.publishInventoryLowStock(event);

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.PRODUCT_EXCHANGE,
                RabbitMQConfig.INVENTORY_LOW_STOCK_ROUTING_KEY,
                event);
    }
}