package com.ekko.order_service.application.service;

import com.ekko.order_service.config.AbstractPostgresIntegrationTest;
import com.ekko.order_service.domain.enums.OrderStatus;
import com.ekko.order_service.domain.event.OrderCancelledEvent;
import com.ekko.order_service.domain.event.OrderConfirmedEvent;
import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.model.OrderDraft;
import com.ekko.order_service.domain.model.PaymentData;
import com.ekko.order_service.domain.model.ProductVariant;
import com.ekko.order_service.domain.port.in.CreateOrderUseCase;
import com.ekko.order_service.domain.port.in.PaymentCallbackUseCase;
import com.ekko.order_service.domain.port.out.OrderEventPublisherPort;
import com.ekko.order_service.domain.port.out.ProductServicePort;
import com.ekko.order_service.infrastructure.persistence.entity.OrderEntity;
import com.ekko.order_service.infrastructure.persistence.entity.OrderPaymentEntity;
import com.ekko.order_service.infrastructure.persistence.entity.OrderStatusHistoryEntity;
import com.ekko.order_service.infrastructure.persistence.enums.ChangedByType;
import com.ekko.order_service.infrastructure.persistence.enums.PaymentStatus;
import com.ekko.order_service.infrastructure.persistence.repository.OrderJpaRepository;
import com.ekko.order_service.infrastructure.persistence.repository.OrderPaymentJpaRepository;
import com.ekko.order_service.infrastructure.persistence.repository.OrderStatusHistoryJpaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.ekko.order_service.builder.OrderTestDataBuilder.anOrderAddress;
import static com.ekko.order_service.util.TestConstants.CUSTOMER_KEYCLOAK_ID;
import static com.ekko.order_service.util.TestConstants.IMAGE_URL;
import static com.ekko.order_service.util.TestConstants.PRODUCT_ID;
import static com.ekko.order_service.util.TestConstants.PRODUCT_NAME;
import static com.ekko.order_service.util.TestConstants.SELLER_KEYCLOAK_ID;
import static com.ekko.order_service.util.TestConstants.SELLER_NAME;
import static com.ekko.order_service.util.TestConstants.VARIANT_ID;
import static com.ekko.order_service.util.TestConstants.VARIANT_SKU;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


class OrderPaymentCallbackServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    private PaymentCallbackUseCase paymentCallbackUseCase;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    private OrderPaymentJpaRepository orderPaymentJpaRepository;

    @Autowired
    private OrderStatusHistoryJpaRepository orderStatusHistoryJpaRepository;

    @MockitoBean
    private ProductServicePort productServicePort;

    @MockitoBean
    private OrderEventPublisherPort orderEventPublisherPort;

    @Test
    void confirmsOrderAfterPaymentPersistingPaymentAndSystemHistoryAndPublishingEvent() {
        Order created = createPendingOrder();
        PaymentData paymentData = new PaymentData(
                UUID.randomUUID(),
                new BigDecimal("200.00"),
                "USD",
                LocalDateTime.of(2025, 8, 9, 13, 0));

        Order confirmed = paymentCallbackUseCase.onPaymentCompleted(created.getId(), paymentData);

        assertEquals(OrderStatus.CONFIRMED, confirmed.getStatus());

        OrderEntity persisted = orderJpaRepository.findById(created.getId()).orElseThrow();
        assertEquals(OrderStatus.CONFIRMED, persisted.getStatus());

        List<OrderPaymentEntity> payments = paymentsFor(created.getId());
        assertEquals(1, payments.size());
        OrderPaymentEntity payment = payments.get(0);
        assertTrue(payment.getIsCurrent());
        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
        assertEquals("USD", payment.getCurrency());
        assertEquals(0, new BigDecimal("200.00").compareTo(payment.getAmount()));
        assertNotNull(payment.getPaymentId());

        OrderStatusHistoryEntity confirmedHistory = historyFor(created.getId()).stream()
                .filter(h -> h.getStatus() == OrderStatus.CONFIRMED)
                .findFirst()
                .orElseThrow();
        assertEquals(ChangedByType.SYSTEM, confirmedHistory.getChangedByType());

        ArgumentCaptor<OrderConfirmedEvent> captor = ArgumentCaptor.forClass(OrderConfirmedEvent.class);
        verify(orderEventPublisherPort, times(1)).publishOrderConfirmed(captor.capture());
        OrderConfirmedEvent event = captor.getValue();
        assertEquals(created.getId(), event.orderId());
        assertEquals(CUSTOMER_KEYCLOAK_ID, event.customerId());
        assertEquals(1, event.items().size());
        assertNotNull(event.items().get(0).orderItemId());
        assertEquals(PRODUCT_ID, event.items().get(0).productId());
        assertEquals(SELLER_KEYCLOAK_ID, event.items().get(0).sellerKeycloakId());
        assertEquals(VARIANT_ID, event.items().get(0).variantId());
        assertEquals(2, event.items().get(0).quantity());
        assertEquals(0, new BigDecimal("200.00").compareTo(event.items().get(0).subtotal()));
    }

    @Test
    void ignoresRepeatedPaymentCompleted() {
        Order created = createPendingOrder();
        PaymentData paymentData = new PaymentData(
                UUID.randomUUID(),
                new BigDecimal("200.00"),
                "USD",
                LocalDateTime.of(2025, 8, 9, 13, 0));

        paymentCallbackUseCase.onPaymentCompleted(created.getId(), paymentData);
        Order result = paymentCallbackUseCase.onPaymentCompleted(created.getId(), paymentData);

        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        assertEquals(1, paymentsFor(created.getId()).size());
        verify(orderEventPublisherPort, times(1)).publishOrderConfirmed(any(OrderConfirmedEvent.class));
    }

    @Test
    void cancelsPendingOrderOnPaymentFailureWithSystemHistory() {
        Order created = createPendingOrder();

        Order cancelled = paymentCallbackUseCase.onPaymentFailed(created.getId());

        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
        assertEquals(OrderStatus.CANCELLED,
                orderJpaRepository.findById(created.getId()).orElseThrow().getStatus());

        OrderStatusHistoryEntity cancelledHistory = historyFor(created.getId()).stream()
                .filter(h -> h.getStatus() == OrderStatus.CANCELLED)
                .findFirst()
                .orElseThrow();
        assertEquals(ChangedByType.SYSTEM, cancelledHistory.getChangedByType());
        assertEquals("Order cancelled after payment failure", cancelledHistory.getNotes());

        verify(orderEventPublisherPort, times(1)).publishOrderCancelled(any(OrderCancelledEvent.class));
        verify(orderEventPublisherPort, times(0)).publishOrderConfirmed(any(OrderConfirmedEvent.class));
    }

    private Order createPendingOrder() {
        ProductVariant variant = new ProductVariant(
                VARIANT_ID,
                PRODUCT_ID,
                PRODUCT_NAME,
                VARIANT_SKU,
                new BigDecimal("100.00"),
                SELLER_KEYCLOAK_ID,
                SELLER_NAME,
                IMAGE_URL,
                10L);
        org.mockito.BDDMockito.given(productServicePort.getVariantsInfo(anyList()))
                .willReturn(List.of(variant));

        OrderDraft draft = new OrderDraft(
                CUSTOMER_KEYCLOAK_ID,
                null,
                "customer@example.com",
                anOrderAddress(),
                List.of(new OrderDraft.OrderItemDraft(VARIANT_ID, 2)),
                "fragile");

        return createOrderUseCase.execute(draft);
    }

    private List<OrderPaymentEntity> paymentsFor(UUID orderId) {
        return orderPaymentJpaRepository.findAll().stream()
                .filter(p -> p.getOrder().getId().equals(orderId))
                .toList();
    }

    private List<OrderStatusHistoryEntity> historyFor(UUID orderId) {
        return orderStatusHistoryJpaRepository.findAll().stream()
                .filter(h -> h.getOrder().getId().equals(orderId))
                .toList();
    }
}