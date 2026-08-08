package com.ekko.order_service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    private UUID id;
    private UUID customerId;
    private String guestEmail;
    private OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal shippingCost;
    private BigDecimal discount;
    private BigDecimal total;
    private String notes;
    private String orderNumber;
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();
    private OrderAddress address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}