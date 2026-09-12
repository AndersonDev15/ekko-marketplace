package com.ekko.seller_service.messaging.event;

import com.ekko.seller_service.config.RabbitMQConfig;
import com.ekko.seller_service.messaging.dto.consume.ProductDeactivatedEvent;
import com.ekko.seller_service.messaging.dto.consume.ProductPublishedEvent;
import com.ekko.seller_service.service.ProductStatusMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductStatusEventListener {

    private final ProductStatusMetricsService metricsService;

    @RabbitListener(queues = RabbitMQConfig.PRODUCT_PUBLISHED_QUEUE)
    public void onProductPublished(ProductPublishedEvent event) {
        metricsService.applyPublished(event);
    }

    @RabbitListener(queues = RabbitMQConfig.PRODUCT_DEACTIVATED_QUEUE)
    public void onProductDeactivated(ProductDeactivatedEvent event) {
        metricsService.applyDeactivated(event);
    }
}