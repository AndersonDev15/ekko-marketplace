package com.ekko.product_service.messaging;

import com.ekko.product_service.config.RabbitMQConfig;
import com.ekko.product_service.messaging.dto.ReviewCreatedEvent;
import com.ekko.product_service.service.ProductRatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewCreatedEventListener {

    private final ProductRatingService productRatingService;

    @RabbitListener(queues = RabbitMQConfig.REVIEW_CREATED_QUEUE)
    public void onReviewCreated(ReviewCreatedEvent event) {
        productRatingService.applyRating(event);
    }
}