package com.ekko.order_service.domain.policy;

import com.ekko.order_service.domain.exception.InvalidOrderStatusTransitionException;
import com.ekko.order_service.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderStatusTransitionPolicyTest {

    private final OrderStatusTransitionPolicy policy = new OrderStatusTransitionPolicy();

    @Test
    void allowsConfirmedToShipped() {
        assertDoesNotThrow(() ->
                policy.assertValidTransition(OrderStatus.CONFIRMED, OrderStatus.SHIPPED));
    }

    @Test
    void allowsShippedToDelivered() {
        assertDoesNotThrow(() ->
                policy.assertValidTransition(OrderStatus.SHIPPED, OrderStatus.DELIVERED));
    }

    @Test
    void rejectsShippedToConfirmed() {
        assertThrows(InvalidOrderStatusTransitionException.class, () ->
                policy.assertValidTransition(OrderStatus.SHIPPED, OrderStatus.CONFIRMED));
    }

    @Test
    void rejectsConfirmedToDeliveredSkip() {
        assertThrows(InvalidOrderStatusTransitionException.class, () ->
                policy.assertValidTransition(OrderStatus.CONFIRMED, OrderStatus.DELIVERED));
    }

    @Test
    void rejectsAnythingFromPending() {
        assertThrows(InvalidOrderStatusTransitionException.class, () ->
                policy.assertValidTransition(OrderStatus.PENDING, OrderStatus.SHIPPED));
        assertThrows(InvalidOrderStatusTransitionException.class, () ->
                policy.assertValidTransition(OrderStatus.PENDING, OrderStatus.DELIVERED));
    }

    @Test
    void rejectsAnyTransitionIntoCancelled() {
        assertThrows(InvalidOrderStatusTransitionException.class, () ->
                policy.assertValidTransition(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        assertThrows(InvalidOrderStatusTransitionException.class, () ->
                policy.assertValidTransition(OrderStatus.SHIPPED, OrderStatus.CANCELLED));
    }

    @Test
    void reportsValidityAsBoolean() {
        assert (policy.isValidTransition(OrderStatus.CONFIRMED, OrderStatus.SHIPPED));
        assert !policy.isValidTransition(OrderStatus.PENDING, OrderStatus.SHIPPED);
    }
}