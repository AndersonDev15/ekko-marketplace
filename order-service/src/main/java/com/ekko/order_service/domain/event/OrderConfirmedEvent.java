package com.ekko.order_service.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderConfirmedEvent(
        UUID orderId,
        String orderNumber,
        UUID customerId,
        String guestEmail,
        List<OrderItemConfirmed> items,
        LocalDateTime confirmedAt
) {
    public record OrderItemConfirmed(
            UUID orderItemId,
            UUID productId,
            UUID variantId,
            Integer quantity,
            UUID sellerKeycloakId,
            BigDecimal subtotal
    ) {
    }
}