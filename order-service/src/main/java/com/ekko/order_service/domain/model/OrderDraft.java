package com.ekko.order_service.domain.model;

import java.util.List;
import java.util.UUID;

public record OrderDraft(
        UUID customerId,
        String guestEmail,
        OrderAddress shippingAddress,
        List<OrderItemDraft> items,
        String notes
) {

    public record OrderItemDraft(UUID variantId, long quantity) {
    }
}