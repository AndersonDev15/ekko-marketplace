package com.ekko.seller_service.messaging;

import com.ekko.seller_service.messaging.dto.SellerCreatedEvent;
import com.ekko.seller_service.messaging.dto.SellerDocumentReviewEvent;
import com.ekko.seller_service.messaging.dto.SellerStatusChangedEvent;

public interface SellerEventPublisher {

    void publishSellerCreated(SellerCreatedEvent event);

    void publishSellerStatusChanged(SellerStatusChangedEvent event);

    void publishSellerDocumentReview(SellerDocumentReviewEvent event);
}