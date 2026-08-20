package com.ekko.review_service.messaging;

import com.ekko.review_service.config.RabbitMQConfig;
import com.ekko.review_service.messaging.dto.publish.ProductRatingUpdatedEvent;
import com.ekko.review_service.messaging.dto.publish.ReviewCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RabbitMQReviewEventPublisher implements ReviewEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishProductRatingUpdated(ProductRatingUpdatedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.REVIEW_EXCHANGE,
                RabbitMQConfig.REVIEW_PRODUCT_RATING_UPDATED_ROUTING_KEY,
                event
        );
    }

    @Override
    public void publishReviewCreated(ReviewCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.REVIEW_EXCHANGE,
                RabbitMQConfig.REVIEW_CREATED_ROUTING_KEY,
                event
        );
    }
}