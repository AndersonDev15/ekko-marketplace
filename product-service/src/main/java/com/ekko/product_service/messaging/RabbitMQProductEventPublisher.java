package com.ekko.product_service.messaging;

import com.ekko.product_service.config.RabbitMQConfig;
import com.ekko.product_service.messaging.dto.publish.InventoryLowStockEvent;
import com.ekko.product_service.messaging.dto.publish.ProductDeactivatedEvent;
import com.ekko.product_service.messaging.dto.publish.ProductPublishedEvent;
import com.ekko.product_service.messaging.dto.publish.ProductRejectedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RabbitMQProductEventPublisher implements ProductEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishProductPublished(ProductPublishedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PRODUCT_EXCHANGE,
                RabbitMQConfig.PRODUCT_PUBLISHED_ROUTING_KEY,
                event
        );
    }

    @Override
    public void publishProductRejected(ProductRejectedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PRODUCT_EXCHANGE,
                RabbitMQConfig.PRODUCT_REJECTED_ROUTING_KEY,
                event
        );
    }

    @Override
    public void publishProductDeactivated(ProductDeactivatedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PRODUCT_EXCHANGE,
                RabbitMQConfig.PRODUCT_DEACTIVATED_ROUTING_KEY,
                event
        );
    }

    @Override
    public void publishInventoryLowStock(InventoryLowStockEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PRODUCT_EXCHANGE,
                RabbitMQConfig.INVENTORY_LOW_STOCK_ROUTING_KEY,
                event
        );
    }
}