package com.ekko.order_service.application.service;

import com.ekko.order_service.domain.exception.InvalidOrderStatusTransitionException;
import com.ekko.order_service.application.exception.OrderNotFoundException;
import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.enums.OrderStatus;
import com.ekko.order_service.domain.event.OrderStatusChangedEvent;
import com.ekko.order_service.domain.policy.OrderStatusTransitionPolicy;
import com.ekko.order_service.domain.port.out.OrderEventPublisherPort;
import com.ekko.order_service.domain.port.out.OrderRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.ekko.order_service.builder.OrderTestDataBuilder.anOrderBuilder;
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
class OrderStatusServiceTest {

    @Mock
    private OrderRepositoryPort orderRepositoryPort;
    @Mock
    private OrderEventPublisherPort orderEventPublisherPort;
    @Mock
    private OrderTransactionService orderTransactionService;

    private OrderStatusService service;

    @BeforeEach
    void setUp() {
        service = new OrderStatusService(
                orderRepositoryPort,
                new OrderStatusTransitionPolicy(),
                orderTransactionService,
                orderEventPublisherPort);
    }

    private void stubFind(Order order) {
        when(orderRepositoryPort.findByOrderNumber(order.getOrderNumber()))
                .thenReturn(Optional.of(order));
    }

    private void stubChangeStatus() {
        when(orderTransactionService.changeStatus(any(Order.class), any(), any(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shipsConfirmedOrderAndPublishesPostCommit() {
        Order order = anOrderBuilder().status(OrderStatus.CONFIRMED).build();
        stubFind(order);
        stubChangeStatus();

        Order result = service.execute(order.getOrderNumber(), OrderStatus.SHIPPED);

        assertEquals(OrderStatus.SHIPPED, result.getStatus());

        InOrder inOrder = inOrder(orderTransactionService, orderEventPublisherPort);
        inOrder.verify(orderTransactionService).changeStatus(
                argThat(changed -> changed.getStatus() == OrderStatus.SHIPPED),
                any(), any(), anyString());
        inOrder.verify(orderEventPublisherPort).publishOrderStatusChanged(
                any(OrderStatusChangedEvent.class));
    }

    @Test
    void deliversShippedOrder() {
        Order order = anOrderBuilder().status(OrderStatus.SHIPPED).build();
        stubFind(order);
        stubChangeStatus();

        Order result = service.execute(order.getOrderNumber(), OrderStatus.DELIVERED);

        assertEquals(OrderStatus.DELIVERED, result.getStatus());

        verify(orderEventPublisherPort).publishOrderStatusChanged(argThat(
                event -> event.previousStatus() == OrderStatus.SHIPPED
                        && event.newStatus() == OrderStatus.DELIVERED));
    }

    @Test
    void rejectsSkipFromConfirmedToDelivered() {
        Order order = anOrderBuilder().status(OrderStatus.CONFIRMED).build();
        stubFind(order);

        assertThrows(InvalidOrderStatusTransitionException.class,
                () -> service.execute(order.getOrderNumber(), OrderStatus.DELIVERED));

        verify(orderTransactionService, never()).changeStatus(any(), any(), any(), anyString());
        verify(orderEventPublisherPort, never()).publishOrderStatusChanged(any());
    }

    @Test
    void rejectsTransitionFromPending() {
        Order order = anOrderBuilder().status(OrderStatus.PENDING).build();
        stubFind(order);

        assertThrows(InvalidOrderStatusTransitionException.class,
                () -> service.execute(order.getOrderNumber(), OrderStatus.SHIPPED));

        verify(orderTransactionService, never()).changeStatus(any(), any(), any(), anyString());
        verify(orderEventPublisherPort, never()).publishOrderStatusChanged(any());
    }

    @Test
    void rejectsUnsupportedTargetStatusSuchAsCancelled() {
        Order order = anOrderBuilder().status(OrderStatus.CONFIRMED).build();
        stubFind(order);

        assertThrows(InvalidOrderStatusTransitionException.class,
                () -> service.execute(order.getOrderNumber(), OrderStatus.CANCELLED));

        verify(orderTransactionService, never()).changeStatus(any(), any(), any(), anyString());
        verify(orderEventPublisherPort, never()).publishOrderStatusChanged(any());
    }

    @Test
    void throwsOrderNotFoundWhenMissing() {
        when(orderRepositoryPort.findByOrderNumber("EKK-00000000-XXXX"))
                .thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,
                () -> service.execute("EKK-00000000-XXXX", OrderStatus.SHIPPED));

        verify(orderTransactionService, never()).changeStatus(any(), any(), any(), anyString());
        verify(orderEventPublisherPort, never()).publishOrderStatusChanged(any());
    }

    @Test
    void doesNotPublishWhenPersistenceFails() {
        Order order = anOrderBuilder().status(OrderStatus.CONFIRMED).build();
        stubFind(order);
        when(orderTransactionService.changeStatus(any(Order.class), any(), any(), anyString()))
                .thenThrow(new RuntimeException("db failure"));

        assertThrows(RuntimeException.class,
                () -> service.execute(order.getOrderNumber(), OrderStatus.SHIPPED));

        verify(orderEventPublisherPort, never()).publishOrderStatusChanged(any());
    }
}