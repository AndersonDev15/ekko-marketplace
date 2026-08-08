package com.ekko.order_service.application.dto;

import com.ekko.order_service.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderDetailResponse(
        UUID id,
        UUID customerId,
        String guestEmail,
        String orderNumber,
        OrderStatus status,
        BigDecimal subtotal,
        BigDecimal shippingCost,
        BigDecimal discount,
        BigDecimal total,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrderItemResponse> items,
        OrderAddressResponse address
) {

    public record OrderItemResponse(
            UUID id,
            UUID variantId,
            UUID productId,
            String productNameSnapshot,
            String variantSnapshot,
            UUID sellerKeycloakId,
            String sellerNameSnapshot,
            BigDecimal priceSnapshot,
            Integer quantity,
            BigDecimal subtotal,
            String imageUrlSnapshot
    ) {
    }

    public record OrderAddressResponse(
            UUID id,
            String fullName,
            String phone,
            String addressLine,
            String city,
            String state,
            String country,
            String postalCode
    ) {
    }
}