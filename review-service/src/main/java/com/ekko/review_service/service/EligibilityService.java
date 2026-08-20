package com.ekko.review_service.service;

import com.ekko.review_service.entity.EligibleReview;
import com.ekko.review_service.messaging.dto.consume.OrderConfirmedEvent;

import java.util.UUID;

public interface EligibilityService {

    EligibleReview assertEligible(String customerId, UUID orderItemId, UUID orderId, UUID productId);

    void registerEligibility(OrderConfirmedEvent event);
}