package com.ekko.seller_service.messaging;

import com.ekko.seller_service.config.RabbitMQConfig;
import com.ekko.seller_service.messaging.dto.consume.OrderConfirmedEvent;
import com.ekko.seller_service.service.OrderConfirmedMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderConfirmedEventListener {

    private final OrderConfirmedMetricsService metricsService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CONFIRMED_QUEUE)
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        metricsService.applyMetrics(event);
    }
}