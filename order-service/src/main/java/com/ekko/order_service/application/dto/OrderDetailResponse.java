package com.ekko.order_service.application.dto;

import com.ekko.order_service.domain.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Full order details")
public record OrderDetailResponse(
        @Schema(description = "Order UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID id,

        @Schema(description = "Customer Keycloak ID (null for guest orders)")
        UUID customerId,

        @Schema(description = "Customer email", example = "customer@example.com")
        String customerEmail,

        @Schema(description = "Human-readable order number", example = "ORD-20240115-ABC123")
        String orderNumber,

        @Schema(description = "Order status", allowableValues = {"PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"})
        OrderStatus status,

        @Schema(description = "Subtotal before shipping and discounts", example = "179.99")
        BigDecimal subtotal,

        @Schema(description = "Shipping cost", example = "19.99")
        BigDecimal shippingCost,

        @Schema(description = "Discount amount", example = "0.00")
        BigDecimal discount,

        @Schema(description = "Total amount (subtotal + shipping - discount)", example = "199.98")
        BigDecimal total,

        @Schema(description = "Order notes", example = "Please leave at the door")
        String notes,

        @Schema(description = "Order creation timestamp")
        LocalDateTime createdAt,

        @Schema(description = "Last update timestamp")
        LocalDateTime updatedAt,

        @Schema(description = "Order items")
        List<OrderItemResponse> items,

        @Schema(description = "Shipping address")
        OrderAddressResponse address
) {

    @Schema(description = "Order item details")
    public record OrderItemResponse(
            @Schema(description = "Order item UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            UUID id,

            @Schema(description = "Product variant UUID", example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
            UUID variantId,

            @Schema(description = "Product UUID", example = "c3d4e5f6-a7b8-9012-cdef-123456789012")
            UUID productId,

            @Schema(description = "Product name at time of order", example = "Wireless Headphones")
            String productNameSnapshot,

            @Schema(description = "Variant description at time of order", example = "Black / Large")
            String variantSnapshot,

            @Schema(description = "Seller Keycloak ID")
            UUID sellerKeycloakId,

            @Schema(description = "Seller name at time of order", example = "TechStore Inc.")
            String sellerNameSnapshot,

            @Schema(description = "Price per unit at time of order", example = "89.99")
            BigDecimal priceSnapshot,

            @Schema(description = "Quantity ordered", example = "2")
            Integer quantity,

            @Schema(description = "Line subtotal (price * quantity)", example = "179.98")
            BigDecimal subtotal,

            @Schema(description = "Product image URL at time of order", example = "https://cdn.example.com/headphones.jpg")
            String imageUrlSnapshot
    ) {
    }

    @Schema(description = "Shipping address")
    public record OrderAddressResponse(
            @Schema(description = "Address UUID", example = "d4e5f6a7-b8c9-0123-def0-123456789012")
            UUID id,

            @Schema(description = "Recipient full name", example = "John Doe")
            String fullName,

            @Schema(description = "Phone number", example = "+1-555-123-4567")
            String phone,

            @Schema(description = "Street address", example = "123 Main St")
            String addressLine,

            @Schema(description = "City", example = "New York")
            String city,

            @Schema(description = "State/Province", example = "NY")
            String state,

            @Schema(description = "Country", example = "USA")
            String country,

            @Schema(description = "Postal/ZIP code", example = "10001")
            String postalCode
    ) {
    }
}