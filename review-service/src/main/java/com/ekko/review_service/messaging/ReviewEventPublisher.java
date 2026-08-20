package com.ekko.review_service.messaging;

import com.ekko.review_service.messaging.dto.publish.ProductRatingUpdatedEvent;
import com.ekko.review_service.messaging.dto.publish.ReviewCreatedEvent;

public interface ReviewEventPublisher {

    void publishProductRatingUpdated(ProductRatingUpdatedEvent event);

    void publishReviewCreated(ReviewCreatedEvent event);
}