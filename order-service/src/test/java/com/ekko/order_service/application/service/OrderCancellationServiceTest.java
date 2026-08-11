package com.ekko.order_service.application.service;

import com.ekko.order_service.domain.exception.OrderAccessDeniedException;
import com.ekko.order_service.domain.exception.OrderCancellationNotAllowedException;
import com.ekko.order_service.domain.exception.OrderNotFoundException;
import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.model.OrderCancelledEvent;
import com.ekko.order_service.domain.model.OrderStatus;
import com.ekko.order_service.domain.policy.OrderCancellationPolicy;
import com.ekko.order_service.domain.policy.OrderOwnershipPolicy;
import com.ekko.order_service.domain.port.out.OrderEventPublisherPort;
import com.ekko.order_service.domain.port.out.OrderRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.ekko.order_service.builder.OrderTestDataBuilder.aGuestOrder;
import static com.ekko.order_service.builder.OrderTestDataBuilder.anOrder;
import static com.ekko.order_service.builder.OrderTestDataBuilder.anOrderBuilder;
import static com.ekko.order_service.util.TestConstants.CUSTOMER_KEYCLOAK_ID;
import static com.ekko.order_service.util.TestConstants.GUEST_EMAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCancellationServiceTest {

    @Mock
    private OrderRepositoryPort orderRepositoryPort;
    @Mock
    private OrderEventPublisherPort orderEventPublisherPort;
    @Mock
    private OrderTransactionService orderTransactionService;

    private OrderCancellationService service;

    @BeforeEach
    void setUp() {
        service = new OrderCancellationService(
                orderRepositoryPort,
                new OrderOwnershipPolicy(),
                new OrderCancellationPolicy(),
                orderTransactionService,
                orderEventPublisherPort);
    }

    @Test
    void cancelsOwnedPendingOrderAndPublishesEventAfterSave() {
        Order order = anOrderBuilder().status(OrderStatus.PENDING).build();
        when(orderRepositoryPort.findByOrderNumber(order.getOrderNumber()))
                .thenReturn(Optional.of(order));
        when(orderTransactionService.cancelOrder(any(Order.class), any(), any(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order result = service.execute(order.getOrderNumber(), CUSTOMER_KEYCLOAK_ID, null, false);

        assertEquals(OrderStatus.CANCELLED, result.getStatus());

        InOrder inOrder = inOrder(orderTransactionService, orderEventPublisherPort);
        inOrder.verify(orderTransactionService).cancelOrder(
                argThat(cancelled -> cancelled.getStatus() == OrderStatus.CANCELLED),
                any(), any(), anyString());
        inOrder.verify(orderEventPublisherPort).publishOrderCancelled(any(OrderCancelledEvent.class));
    }

    @Test
    void publishesRefundRequiredForPreviouslyConfirmedOrder() {
        Order order = anOrderBuilder().status(OrderStatus.CONFIRMED).build();
        when(orderRepositoryPort.findByOrderNumber(order.getOrderNumber()))
                .thenReturn(Optional.of(order));
        when(orderTransactionService.cancelOrder(any(Order.class), any(), any(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.execute(order.getOrderNumber(), CUSTOMER_KEYCLOAK_ID, null, false);

        verify(orderEventPublisherPort).publishOrderCancelled(argThat(
                event -> event.previousStatus() == OrderStatus.CONFIRMED
                        && event.refundRequired()));
    }

    @Test
    void publishesNoRefundForPreviouslyPendingOrder() {
        Order order = anOrderBuilder().status(OrderStatus.PENDING).build();
        when(orderRepositoryPort.findByOrderNumber(order.getOrderNumber()))
                .thenReturn(Optional.of(order));
        when(orderTransactionService.cancelOrder(any(Order.class), any(), any(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.execute(order.getOrderNumber(), CUSTOMER_KEYCLOAK_ID, null, false);

        verify(orderEventPublisherPort).publishOrderCancelled(argThat(
                event -> event.previousStatus() == OrderStatus.PENDING
                        && !event.refundRequired()));
    }

    @Test
    void cancelsGuestOrderWithMatchingEmail() {
        Order order = aGuestOrder();
        when(orderRepositoryPort.findByOrderNumber(order.getOrderNumber()))
                .thenReturn(Optional.of(order));
        when(orderTransactionService.cancelOrder(any(Order.class), any(), any(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order result = service.execute(order.getOrderNumber(), null, GUEST_EMAIL, false);

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
    }

    @Test
    void rejectsGuestWithDifferentEmail() {
        Order order = aGuestOrder();
        when(orderRepositoryPort.findByOrderNumber(order.getOrderNumber()))
                .thenReturn(Optional.of(order));

        assertThrows(OrderAccessDeniedException.class,
                () -> service.execute(order.getOrderNumber(), null, "other@example.com", false));

        verify(orderTransactionService, never()).cancelOrder(any(), any(), any(), anyString());
        verify(orderEventPublisherPort, never()).publishOrderCancelled(any(OrderCancelledEvent.class));
    }

    @Test
    void rejectsCustomerThatDoesNotOwnOrder() {
        Order order = anOrder();
        when(orderRepositoryPort.findByOrderNumber(order.getOrderNumber()))
                .thenReturn(Optional.of(order));

        assertThrows(OrderAccessDeniedException.class,
                () -> service.execute(order.getOrderNumber(), UUID.randomUUID(), null, false));

        verify(orderTransactionService, never()).cancelOrder(any(), any(), any(), anyString());
        verify(orderEventPublisherPort, never()).publishOrderCancelled(any(OrderCancelledEvent.class));
    }

    @Test
    void adminCanCancelWithoutOwnershipCheck() {
        Order order = anOrderBuilder().status(OrderStatus.PENDING)
                .customerId(CUSTOMER_KEYCLOAK_ID).build();
        when(orderRepositoryPort.findByOrderNumber(order.getOrderNumber()))
                .thenReturn(Optional.of(order));
        when(orderTransactionService.cancelOrder(any(Order.class), any(), any(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order result = service.execute(order.getOrderNumber(), UUID.randomUUID(), null, true);

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
    }

    @Test
    void adminCannotCancelShippedOrder() {
        Order order = anOrderBuilder().status(OrderStatus.SHIPPED).build();
        when(orderRepositoryPort.findByOrderNumber(order.getOrderNumber()))
                .thenReturn(Optional.of(order));

        assertThrows(OrderCancellationNotAllowedException.class,
                () -> service.execute(order.getOrderNumber(), null, null, true));

        verify(orderTransactionService, never()).cancelOrder(any(), any(), any(), anyString());
        verify(orderEventPublisherPort, never()).publishOrderCancelled(any(OrderCancelledEvent.class));
    }

    @Test
    void customerCannotCancelShippedOrder() {
        Order order = anOrderBuilder().status(OrderStatus.SHIPPED)
                .customerId(CUSTOMER_KEYCLOAK_ID).build();
        when(orderRepositoryPort.findByOrderNumber(order.getOrderNumber()))
                .thenReturn(Optional.of(order));

        assertThrows(OrderCancellationNotAllowedException.class,
                () -> service.execute(order.getOrderNumber(), CUSTOMER_KEYCLOAK_ID, null, false));
    }

    @Test
    void throwsOrderNotFoundWhenOrderMissing() {
        when(orderRepositoryPort.findByOrderNumber("EKK-00000000-XXXX"))
                .thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,
                () -> service.execute("EKK-00000000-XXXX", null, null, true));

        verify(orderTransactionService, never()).cancelOrder(any(), any(), any(), anyString());
        verify(orderEventPublisherPort, never()).publishOrderCancelled(any(OrderCancelledEvent.class));
    }

    @Test
    void doesNotPublishEventWhenPersistenceFails() {
        Order order = anOrderBuilder().status(OrderStatus.PENDING).build();
        when(orderRepositoryPort.findByOrderNumber(order.getOrderNumber()))
                .thenReturn(Optional.of(order));
        when(orderTransactionService.cancelOrder(any(Order.class), any(), any(), anyString()))
                .thenThrow(new RuntimeException("db failure"));

        assertThrows(RuntimeException.class,
                () -> service.execute(order.getOrderNumber(), CUSTOMER_KEYCLOAK_ID, null, false));

        verify(orderEventPublisherPort, never()).publishOrderCancelled(any(OrderCancelledEvent.class));
    }
}