package com.ekko.order_service.domain.model;

import com.ekko.order_service.domain.enums.OrderStatus;
import com.ekko.order_service.domain.exception.InvalidOrderStatusTransitionException;
import com.ekko.order_service.domain.policy.OrderStatusTransitionPolicy;
import org.junit.jupiter.api.Test;

import static com.ekko.order_service.builder.OrderTestDataBuilder.anOrderBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderTest {

    private final OrderStatusTransitionPolicy transitionPolicy = new OrderStatusTransitionPolicy();

    @Test
    void cancelTransitionsStatusToCancelled() {
        Order order = anOrderBuilder().status(OrderStatus.CONFIRMED).build();

        order.cancel();

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void cancelRefreshesUpdatedAt() {
        Order order = anOrderBuilder().build();

        order.cancel();

        assertNotEquals(order.getCreatedAt(), order.getUpdatedAt());
    }

    @Test
    void shipTransitionsFromConfirmedToShipped() {
        Order order = anOrderBuilder().status(OrderStatus.CONFIRMED).build();

        order.ship(transitionPolicy);

        assertEquals(OrderStatus.SHIPPED, order.getStatus());
    }

    @Test
    void shipRejectsAnyOtherFromStatus() {
        Order order = anOrderBuilder().status(OrderStatus.DELIVERED).build();

        assertThrows(InvalidOrderStatusTransitionException.class,
                () -> order.ship(transitionPolicy));
    }

    @Test
    void deliverTransitionsFromShippedToDelivered() {
        Order order = anOrderBuilder().status(OrderStatus.SHIPPED).build();

        order.deliver(transitionPolicy);

        assertEquals(OrderStatus.DELIVERED, order.getStatus());
    }

    @Test
    void deliverRejectsAnyOtherFromStatus() {
        Order order = anOrderBuilder().status(OrderStatus.CONFIRMED).build();

        assertThrows(InvalidOrderStatusTransitionException.class,
                () -> order.deliver(transitionPolicy));
    }
}