package com.ekko.review_service.service;

import com.ekko.review_service.messaging.dto.OrderConfirmedEvent;

import java.util.UUID;

public interface EligibilityService {

    void assertEligible(String customerId, UUID orderItemId, UUID orderId, UUID productId);

    void registerEligibility(OrderConfirmedEvent event);
}