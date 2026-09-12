package com.ekko.seller_service.messaging.publish;

import com.ekko.seller_service.messaging.dto.publish.SellerCreatedEvent;
import com.ekko.seller_service.messaging.dto.publish.SellerDocumentReviewEvent;
import com.ekko.seller_service.messaging.dto.publish.SellerSlugChangedEvent;
import com.ekko.seller_service.messaging.dto.publish.SellerStatusChangedEvent;

public interface SellerEventPublisher {

    void publishSellerCreated(SellerCreatedEvent event);

    void publishSellerStatusChanged(SellerStatusChangedEvent event);

    void publishSellerDocumentReview(SellerDocumentReviewEvent event);

    void publishSellerSlugChanged(SellerSlugChangedEvent event);
}