package com.ekko.order_service.application.service;

import com.ekko.order_service.application.exception.OrderNotFoundException;
import com.ekko.order_service.domain.enums.OrderStatus;
import com.ekko.order_service.domain.event.OrderConfirmedEvent;
import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.model.PaymentData;
import com.ekko.order_service.domain.port.out.OrderEventPublisherPort;
import com.ekko.order_service.domain.port.out.OrderRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ekko.order_service.builder.OrderTestDataBuilder.anOrderBuilder;
import static com.ekko.order_service.builder.OrderTestDataBuilder.anOrderItem;
import static com.ekko.order_service.util.TestConstants.CUSTOMER_KEYCLOAK_ID;
import static com.ekko.order_service.util.TestConstants.PRODUCT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPaymentCallbackServiceTest {

    @Mock
    private OrderRepositoryPort orderRepositoryPort;
    @Mock
    private OrderTransactionService orderTransactionService;
    @Mock
    private OrderCancellationService orderCancellationService;
    @Mock
    private OrderEventPublisherPort orderEventPublisherPort;

    private OrderPaymentCallbackService service;

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID ITEM_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new OrderPaymentCallbackService(
                orderRepositoryPort,
                orderTransactionService,
                orderCancellationService,
                orderEventPublisherPort);
    }

    @Test
    void confirmsPendingOrderPersistsThenPublishesAfterCommit() {
        Order order = anOrderBuilder().id(ORDER_ID).status(OrderStatus.PENDING).build();
        when(orderRepositoryPort.findById(ORDER_ID)).thenReturn(Optional.of(order));
        Order saved = confirmedOrder();
        PaymentData paymentData = paymentData();
        when(orderTransactionService.confirmOrder(any(Order.class), any(PaymentData.class)))
                .thenReturn(saved);

        Order result = service.onPaymentCompleted(ORDER_ID, paymentData);

        assertSame(saved, result);
        assertEquals(OrderStatus.CONFIRMED, saved.getStatus());

        InOrder inOrder = inOrder(orderTransactionService, orderEventPublisherPort);
        inOrder.verify(orderTransactionService).confirmOrder(
                argThat(confirmed -> confirmed.getStatus() == OrderStatus.CONFIRMED),
                argThat(data -> data.paymentId().equals(paymentData.paymentId())));
        inOrder.verify(orderEventPublisherPort).publishOrderConfirmed(any(OrderConfirmedEvent.class));
    }

    @Test
    void publishesConfirmedEventWithOrderItemIds() {
        Order order = anOrderBuilder().id(ORDER_ID).status(OrderStatus.PENDING).build();
        when(orderRepositoryPort.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderTransactionService.confirmOrder(any(Order.class), any(PaymentData.class)))
                .thenReturn(confirmedOrder());

        service.onPaymentCompleted(ORDER_ID, paymentData());

        verify(orderEventPublisherPort).publishOrderConfirmed(argThat(
                event -> event.orderId().equals(ORDER_ID)
                        && event.customerId().equals(CUSTOMER_KEYCLOAK_ID)
                        && event.items().size() == 1
                        && event.items().get(0).orderItemId().equals(ITEM_ID)
                        && event.items().get(0).productId().equals(PRODUCT_ID)));
    }

    @Test
    void ignoresAlreadyConfirmedOrder() {
        Order order = anOrderBuilder().id(ORDER_ID).status(OrderStatus.CONFIRMED).build();
        when(orderRepositoryPort.findById(ORDER_ID)).thenReturn(Optional.of(order));

        Order result = service.onPaymentCompleted(ORDER_ID, paymentData());

        assertSame(order, result);
        verify(orderTransactionService, never()).confirmOrder(any(), any());
        verify(orderEventPublisherPort, never()).publishOrderConfirmed(any(OrderConfirmedEvent.class));
    }

    @Test
    void ignoresCancelledOrder() {
        Order order = anOrderBuilder().id(ORDER_ID).status(OrderStatus.CANCELLED).build();
        when(orderRepositoryPort.findById(ORDER_ID)).thenReturn(Optional.of(order));

        Order result = service.onPaymentCompleted(ORDER_ID, paymentData());

        assertSame(order, result);
        verify(orderTransactionService, never()).confirmOrder(any(), any());
        verify(orderEventPublisherPort, never()).publishOrderConfirmed(any(OrderConfirmedEvent.class));
    }

    @Test
    void throwsOrderNotFoundWhenOrderMissing() {
        when(orderRepositoryPort.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,
                () -> service.onPaymentCompleted(ORDER_ID, paymentData()));

        verify(orderTransactionService, never()).confirmOrder(any(), any());
        verify(orderEventPublisherPort, never()).publishOrderConfirmed(any(OrderConfirmedEvent.class));
    }

    @Test
    void doesNotPublishWhenPersistenceFails() {
        Order order = anOrderBuilder().id(ORDER_ID).status(OrderStatus.PENDING).build();
        when(orderRepositoryPort.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderTransactionService.confirmOrder(any(Order.class), any(PaymentData.class)))
                .thenThrow(new RuntimeException("db failure"));

        assertThrows(RuntimeException.class,
                () -> service.onPaymentCompleted(ORDER_ID, paymentData()));

        verify(orderEventPublisherPort, never()).publishOrderConfirmed(any(OrderConfirmedEvent.class));
    }

    @Test
    void delegatesCancellationForPendingOrderOnPaymentFailure() {
        Order order = anOrderBuilder().id(ORDER_ID).status(OrderStatus.PENDING).build();
        Order cancelled = anOrderBuilder().id(ORDER_ID).status(OrderStatus.CANCELLED).build();
        when(orderRepositoryPort.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderCancellationService.cancelBySystem(order.getOrderNumber())).thenReturn(cancelled);

        Order result = service.onPaymentFailed(ORDER_ID);

        assertSame(cancelled, result);
        verify(orderCancellationService).cancelBySystem(order.getOrderNumber());
    }

    @Test
    void ignoresNonPendingOrderOnPaymentFailure() {
        Order order = anOrderBuilder().id(ORDER_ID).status(OrderStatus.CONFIRMED).build();
        when(orderRepositoryPort.findById(ORDER_ID)).thenReturn(Optional.of(order));

        Order result = service.onPaymentFailed(ORDER_ID);

        assertSame(order, result);
        verify(orderCancellationService, never()).cancelBySystem(any());
    }

    @Test
    void throwsOrderNotFoundOnPaymentFailureWhenOrderMissing() {
        when(orderRepositoryPort.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,
                () -> service.onPaymentFailed(ORDER_ID));

        verify(orderCancellationService, never()).cancelBySystem(any());
    }

    private PaymentData paymentData() {
        return new PaymentData(
                UUID.randomUUID(),
                new BigDecimal("200.00"),
                "USD",
                LocalDateTime.of(2025, 8, 9, 13, 0));
    }

    private Order confirmedOrder() {
        Order base = anOrderBuilder().id(ORDER_ID).status(OrderStatus.PENDING).build();
        return Order.builder()
                .id(ORDER_ID)
                .customerId(base.getCustomerId())
                .customerEmail(base.getCustomerEmail())
                .status(OrderStatus.CONFIRMED)
                .subtotal(base.getSubtotal())
                .shippingCost(base.getShippingCost())
                .discount(base.getDiscount())
                .total(base.getTotal())
                .notes(base.getNotes())
                .orderNumber(base.getOrderNumber())
                .items(new ArrayList<>(List.of(anOrderItemWithId(ITEM_ID))))
                .address(base.getAddress())
                .createdAt(base.getCreatedAt())
                .updatedAt(LocalDateTime.of(2025, 8, 9, 13, 5))
                .build();
    }

    private static com.ekko.order_service.domain.model.OrderItem anOrderItemWithId(UUID id) {
        com.ekko.order_service.domain.model.OrderItem item = anOrderItem(2, new BigDecimal("100.00"));
        return new com.ekko.order_service.domain.model.OrderItem(
                id,
                item.variantId(),
                item.productId(),
                item.productNameSnapshot(),
                item.variantSnapshot(),
                item.sellerKeycloakId(),
                item.sellerNameSnapshot(),
                item.priceSnapshot(),
                item.quantity(),
                item.subtotal(),
                item.imageUrlSnapshot());
    }
}