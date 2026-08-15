package com.ekko.review_service.messaging;

import com.ekko.review_service.messaging.dto.ProductRatingUpdatedEvent;

public interface ReviewEventPublisher {

    void publishProductRatingUpdated(ProductRatingUpdatedEvent event);
}