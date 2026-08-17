package com.ekko.order_service.builder;

import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.model.OrderAddress;
import com.ekko.order_service.domain.model.OrderItem;
import com.ekko.order_service.domain.enums.OrderStatus;
import com.ekko.order_service.domain.model.ProductVariant;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.ekko.order_service.util.TestConstants.ADDRESS_CITY;
import static com.ekko.order_service.util.TestConstants.ADDRESS_COUNTRY;
import static com.ekko.order_service.util.TestConstants.ADDRESS_FULL_NAME;
import static com.ekko.order_service.util.TestConstants.ADDRESS_LINE;
import static com.ekko.order_service.util.TestConstants.ADDRESS_PHONE;
import static com.ekko.order_service.util.TestConstants.ADDRESS_POSTAL_CODE;
import static com.ekko.order_service.util.TestConstants.ADDRESS_STATE;
import static com.ekko.order_service.util.TestConstants.CUSTOMER_KEYCLOAK_ID;
import static com.ekko.order_service.util.TestConstants.GUEST_EMAIL;
import static com.ekko.order_service.util.TestConstants.PRODUCT_ID;
import static com.ekko.order_service.util.TestConstants.PRODUCT_NAME;
import static com.ekko.order_service.util.TestConstants.SELLER_KEYCLOAK_ID;
import static com.ekko.order_service.util.TestConstants.SELLER_NAME;
import static com.ekko.order_service.util.TestConstants.VARIANT_ID;
import static com.ekko.order_service.util.TestConstants.VARIANT_SKU;

public final class OrderTestDataBuilder {

    private OrderTestDataBuilder() {
    }

    public static OrderAddress anOrderAddress() {
        return new OrderAddress(
                null,
                ADDRESS_FULL_NAME,
                ADDRESS_PHONE,
                ADDRESS_LINE,
                ADDRESS_CITY,
                ADDRESS_STATE,
                ADDRESS_COUNTRY,
                ADDRESS_POSTAL_CODE);
    }

    public static OrderItem anOrderItem(int quantity, BigDecimal price) {
        return new OrderItem(
                null,
                VARIANT_ID,
                PRODUCT_ID,
                PRODUCT_NAME,
                VARIANT_SKU,
                SELLER_KEYCLOAK_ID,
                SELLER_NAME,
                price,
                quantity,
                price.multiply(BigDecimal.valueOf(quantity)),
                "https://cdn.example.com/variant.jpg");
    }

    public static ProductVariant aProductVariant(long availableStock) {
        return new ProductVariant(
                VARIANT_ID,
                PRODUCT_ID,
                PRODUCT_NAME,
                VARIANT_SKU,
                new BigDecimal("100.00"),
                SELLER_KEYCLOAK_ID,
                SELLER_NAME,
                "https://cdn.example.com/variant.jpg",
                availableStock);
    }

    public static Order anOrder() {
        return anOrderBuilder().build();
    }

    public static Order.OrderBuilder anOrderBuilder() {
        return Order.builder()
                .id(UUID.randomUUID())
                .customerId(CUSTOMER_KEYCLOAK_ID)
                .guestEmail(null)
                .status(OrderStatus.PENDING)
                .subtotal(new BigDecimal("200.00"))
                .shippingCost(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .total(new BigDecimal("200.00"))
                .notes(null)
                .orderNumber("EKK-20250809-AB12")
                .items(new ArrayList<>(List.of(anOrderItem(2, new BigDecimal("100.00")))))
                .address(anOrderAddress())
                .createdAt(LocalDateTime.of(2025, 8, 9, 12, 0))
                .updatedAt(LocalDateTime.of(2025, 8, 9, 12, 0));
    }

    public static Order aGuestOrder() {
        return anOrderBuilder()
                .customerId(null)
                .guestEmail(GUEST_EMAIL)
                .build();
    }
}