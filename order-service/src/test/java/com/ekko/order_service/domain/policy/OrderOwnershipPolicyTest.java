package com.ekko.order_service.domain.policy;

import com.ekko.order_service.domain.exception.OrderAccessDeniedException;
import com.ekko.order_service.domain.model.Order;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.ekko.order_service.builder.OrderTestDataBuilder.aGuestOrder;
import static com.ekko.order_service.builder.OrderTestDataBuilder.anOrder;
import static com.ekko.order_service.builder.OrderTestDataBuilder.anOrderBuilder;
import static com.ekko.order_service.util.TestConstants.CUSTOMER_KEYCLOAK_ID;
import static com.ekko.order_service.util.TestConstants.GUEST_EMAIL;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderOwnershipPolicyTest {

    private final OrderOwnershipPolicy policy = new OrderOwnershipPolicy();

    @Test
    void allowsCustomerWithMatchingKeycloakId() {
        Order order = anOrder();

        assertDoesNotThrow(
                () -> policy.assertCustomerOwns(order, CUSTOMER_KEYCLOAK_ID));
    }

    @Test
    void rejectsCustomerWithDifferentKeycloakId() {
        Order order = anOrder();

        assertThrows(OrderAccessDeniedException.class,
                () -> policy.assertCustomerOwns(order, UUID.randomUUID()));
    }

    @Test
    void rejectsCustomerWhenOrderHasNoCustomerId() {
        Order order = aGuestOrder();

        assertThrows(OrderAccessDeniedException.class,
                () -> policy.assertCustomerOwns(order, CUSTOMER_KEYCLOAK_ID));
    }

    @Test
    void rejectsCustomerWhenKeycloakIdIsNull() {
        Order order = anOrder();

        assertThrows(OrderAccessDeniedException.class,
                () -> policy.assertCustomerOwns(order, null));
    }

    @Test
    void allowsGuestWithMatchingEmail() {
        Order order = aGuestOrder();

        assertDoesNotThrow(() -> policy.assertGuestOwns(order, GUEST_EMAIL));
    }

    @Test
    void rejectsGuestWithDifferentEmail() {
        Order order = aGuestOrder();

        assertThrows(OrderAccessDeniedException.class,
                () -> policy.assertGuestOwns(order, "other@example.com"));
    }

    @Test
    void rejectsGuestWhenOrderHasNoGuestEmail() {
        Order order = anOrderBuilder().customerId(CUSTOMER_KEYCLOAK_ID).build();

        assertThrows(OrderAccessDeniedException.class,
                () -> policy.assertGuestOwns(order, GUEST_EMAIL));
    }
}