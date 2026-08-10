package com.ekko.order_service.domain.policy;

import com.ekko.order_service.domain.exception.OrderCancellationNotAllowedException;
import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;

import static com.ekko.order_service.builder.OrderTestDataBuilder.anOrderBuilder;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderCancellationPolicyTest {

    private final OrderCancellationPolicy policy = new OrderCancellationPolicy();

    @Test
    void allowsCancellationWhenPending() {
        Order order = anOrderBuilder().status(OrderStatus.PENDING).build();

        assertDoesNotThrow(() -> policy.assertCancellable(order));
    }

    @Test
    void allowsCancellationWhenConfirmed() {
        Order order = anOrderBuilder().status(OrderStatus.CONFIRMED).build();

        assertDoesNotThrow(() -> policy.assertCancellable(order));
    }

    @Test
    void rejectsCancellationWhenShipped() {
        assertNotCancellable(OrderStatus.SHIPPED);
    }

    @Test
    void rejectsCancellationWhenDelivered() {
        assertNotCancellable(OrderStatus.DELIVERED);
    }

    @Test
    void rejectsCancellationWhenAlreadyCancelled() {
        assertNotCancellable(OrderStatus.CANCELLED);
    }

    private void assertNotCancellable(OrderStatus status) {
        Order order = anOrderBuilder().status(status).build();

        OrderCancellationNotAllowedException exception =
                assertThrows(OrderCancellationNotAllowedException.class,
                        () -> policy.assertCancellable(order));

        assertEquals(status, extractStatus(exception.getMessage()));
    }

    private OrderStatus extractStatus(String message) {
        String prefix = "Order cannot be cancelled in status: ";
        return OrderStatus.valueOf(message.substring(prefix.length()).trim());
    }
}