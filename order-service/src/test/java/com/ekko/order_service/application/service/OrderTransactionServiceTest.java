package com.ekko.order_service.application.service;

import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.model.OrderChangeSource;
import com.ekko.order_service.domain.model.OrderStatus;
import com.ekko.order_service.domain.port.out.OrderRepositoryPort;
import com.ekko.order_service.domain.port.out.OrderStatusHistoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static com.ekko.order_service.builder.OrderTestDataBuilder.anOrder;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderTransactionServiceTest {

    @Mock
    private OrderRepositoryPort orderRepositoryPort;
    @Mock
    private OrderStatusHistoryPort orderStatusHistoryPort;

    @InjectMocks
    private OrderTransactionService service;

    @Test
    void persistsOrderAndRecordsInitialHistory() {
        Order order = anOrder();
        UUID savedId = UUID.randomUUID();
        Order saved = anOrderBuilderWithId(savedId);
        when(orderRepositoryPort.save(order)).thenReturn(saved);

        Order result = service.commitOrder(order);

        assertSame(saved, result);
        InOrder inOrder = inOrder(orderRepositoryPort, orderStatusHistoryPort);
        inOrder.verify(orderRepositoryPort).save(order);
        inOrder.verify(orderStatusHistoryPort).recordStatusChange(
                savedId,
                OrderStatus.PENDING,
                OrderChangeSource.SYSTEM,
                null,
                "Order created");
    }

    @Test
    void doesNotRecordHistoryWhenSaveFails() {
        when(orderRepositoryPort.save(any(Order.class)))
                .thenThrow(new RuntimeException("db failure"));

        assertThrows(RuntimeException.class, () -> service.commitOrder(anOrder()));

        verify(orderStatusHistoryPort, never()).recordStatusChange(
                any(), any(), any(), isNull(), eq("Order created"));
    }

    @Test
    void cancelsOrderAndRecordsCancellationHistory() {
        Order order = anOrder();
        UUID savedId = UUID.randomUUID();
        UUID changedBy = UUID.randomUUID();
        Order saved = anOrderBuilderWithId(savedId);
        when(orderRepositoryPort.save(order)).thenReturn(saved);

        Order result = service.cancelOrder(
                order, OrderChangeSource.CUSTOMER, changedBy, "Order cancelled");

        assertSame(saved, result);
        InOrder inOrder = inOrder(orderRepositoryPort, orderStatusHistoryPort);
        inOrder.verify(orderRepositoryPort).save(order);
        inOrder.verify(orderStatusHistoryPort).recordStatusChange(
                savedId,
                OrderStatus.CANCELLED,
                OrderChangeSource.CUSTOMER,
                changedBy,
                "Order cancelled");
    }

    @Test
    void doesNotRecordHistoryWhenCancellationFails() {
        when(orderRepositoryPort.save(any(Order.class)))
                .thenThrow(new RuntimeException("db failure"));

        assertThrows(RuntimeException.class, () -> service.cancelOrder(
                anOrder(), OrderChangeSource.ADMIN, null, "Order cancelled by admin"));

        verify(orderStatusHistoryPort, never()).recordStatusChange(
                any(), any(), any(), isNull(), eq("Order cancelled by admin"));
    }

    @Test
    void changesStatusAndRecordsHistoryWithProvidedSource() {
        Order order = anOrderBuilderWithStatus(OrderStatus.SHIPPED);
        UUID savedId = UUID.randomUUID();
        UUID changedBy = UUID.randomUUID();
        Order saved = anOrderBuilderWithId(savedId, OrderStatus.SHIPPED);
        when(orderRepositoryPort.save(order)).thenReturn(saved);

        Order result = service.changeStatus(
                order, OrderChangeSource.ADMIN, changedBy, "manual update");

        assertSame(saved, result);
        InOrder inOrder = inOrder(orderRepositoryPort, orderStatusHistoryPort);
        inOrder.verify(orderRepositoryPort).save(order);
        inOrder.verify(orderStatusHistoryPort).recordStatusChange(
                savedId,
                OrderStatus.SHIPPED,
                OrderChangeSource.ADMIN,
                changedBy,
                "manual update");
    }

    private static Order anOrderBuilderWithStatus(OrderStatus status) {
        return anOrderBuilderWithId(UUID.randomUUID(), status);
    }

    private static Order anOrderBuilderWithId(UUID id, OrderStatus status) {
        Order base = anOrder();
        return Order.builder()
                .id(id)
                .customerId(base.getCustomerId())
                .guestEmail(base.getGuestEmail())
                .status(status)
                .subtotal(base.getSubtotal())
                .shippingCost(base.getShippingCost())
                .discount(base.getDiscount())
                .total(base.getTotal())
                .notes(base.getNotes())
                .orderNumber(base.getOrderNumber())
                .items(base.getItems())
                .address(base.getAddress())
                .createdAt(base.getCreatedAt())
                .updatedAt(base.getUpdatedAt())
                .build();
    }

    private static Order anOrderBuilderWithId(UUID id) {
        return anOrderBuilderWithId(id, anOrder().getStatus());
    }
}