package com.ekko.seller_service.messaging.event;

import com.ekko.seller_service.config.RabbitMQConfig;
import com.ekko.seller_service.messaging.dto.consume.ReviewCreatedEvent;
import com.ekko.seller_service.service.ReviewCreatedMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewCreatedEventListener {

    private final ReviewCreatedMetricsService metricsService;

    @RabbitListener(queues = RabbitMQConfig.REVIEW_CREATED_QUEUE)
    public void onReviewCreated(ReviewCreatedEvent event) {
        metricsService.applyMetrics(event);
    }
}