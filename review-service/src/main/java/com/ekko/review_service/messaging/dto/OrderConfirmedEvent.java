package com.ekko.review_service.messaging.dto;

import java.util.List;
import java.util.UUID;

/**
 * PLACEHOLDER event: order-service does NOT publish this yet.
 * Expected shape for the future order.confirmed event; the real contract must be validated
 * against order-service before going live. In particular, the current order.created items
 * carry variantId/productId but NO orderItemId, so order-service must add orderItemId to the
 * confirmed event for this consumer to work.
 */
public record OrderConfirmedEvent(
        UUID orderId,
        String customerId,
        List<OrderItemConfirmed> items
) {

    public record OrderItemConfirmed(
            UUID orderItemId,
            UUID productId
    ) {
    }
}