package com.ekko.order_service.domain.model;

import com.ekko.order_service.domain.exception.InvalidOrderStatusTransitionException;
import com.ekko.order_service.domain.policy.OrderStatusTransitionPolicy;
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

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    public void ship(OrderStatusTransitionPolicy transitionPolicy) {
        validateTransition(transitionPolicy, OrderStatus.SHIPPED);
        this.status = OrderStatus.SHIPPED;
        this.updatedAt = LocalDateTime.now();
    }

    public void deliver(OrderStatusTransitionPolicy transitionPolicy) {
        validateTransition(transitionPolicy, OrderStatus.DELIVERED);
        this.status = OrderStatus.DELIVERED;
        this.updatedAt = LocalDateTime.now();
    }

    private void validateTransition(OrderStatusTransitionPolicy policy, OrderStatus to) {
        if (!policy.isValidTransition(this.status, to)) {
            throw new InvalidOrderStatusTransitionException(this.status, to);
        }
    }
}