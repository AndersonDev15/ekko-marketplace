package com.ekko.order_service.application.service;

import com.ekko.order_service.config.AbstractPostgresIntegrationTest;
import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.event.OrderCreatedEvent;
import com.ekko.order_service.domain.model.OrderDraft;
import com.ekko.order_service.domain.enums.OrderStatus;
import com.ekko.order_service.domain.model.ProductVariant;
import com.ekko.order_service.domain.port.in.CreateOrderUseCase;
import com.ekko.order_service.domain.port.out.OrderEventPublisherPort;
import com.ekko.order_service.domain.port.out.ProductServicePort;
import com.ekko.order_service.infrastructure.persistence.enums.ChangedByType;
import com.ekko.order_service.infrastructure.persistence.entity.OrderEntity;
import com.ekko.order_service.infrastructure.persistence.entity.OrderStatusHistoryEntity;
import com.ekko.order_service.infrastructure.persistence.repository.OrderJpaRepository;
import com.ekko.order_service.infrastructure.persistence.repository.OrderStatusHistoryJpaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import static com.ekko.order_service.builder.OrderTestDataBuilder.anOrderAddress;
import static com.ekko.order_service.util.TestConstants.CUSTOMER_KEYCLOAK_ID;
import static com.ekko.order_service.util.TestConstants.PRODUCT_ID;
import static com.ekko.order_service.util.TestConstants.PRODUCT_NAME;
import static com.ekko.order_service.util.TestConstants.IMAGE_URL;
import static com.ekko.order_service.util.TestConstants.SELLER_KEYCLOAK_ID;
import static com.ekko.order_service.util.TestConstants.SELLER_NAME;
import static com.ekko.order_service.util.TestConstants.VARIANT_ID;
import static com.ekko.order_service.util.TestConstants.VARIANT_SKU;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class OrderCreationServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    private OrderStatusHistoryJpaRepository orderStatusHistoryJpaRepository;

    @MockitoBean
    private ProductServicePort productServicePort;

    @MockitoBean
    private OrderEventPublisherPort orderEventPublisherPort;

    @Test
    void persistsOrderWithInitialHistoryAndPublishesEventAfterCommit() {
        whenVariantAvailable();
        OrderDraft draft = new OrderDraft(
                CUSTOMER_KEYCLOAK_ID,
                null,
                null,
                anOrderAddress(),
                List.of(new OrderDraft.OrderItemDraft(VARIANT_ID, 2)),
                "fragile");

        Order created = createOrderUseCase.execute(draft);

        assertNotNull(created.getId());
        assertEquals(OrderStatus.PENDING, created.getStatus());

        OrderEntity persisted = orderJpaRepository.findByOrderNumber(created.getOrderNumber()).orElseThrow();
        assertEquals(created.getId(), persisted.getId());
        assertEquals(OrderStatus.PENDING, persisted.getStatus());
        assertEquals(new BigDecimal("200.00").compareTo(persisted.getTotal()), 0);

        List<OrderStatusHistoryEntity> history =
                orderStatusHistoryJpaRepository.findAll().stream()
                        .filter(h -> h.getOrder().getId().equals(created.getId()))
                        .toList();
        assertEquals(1, history.size());
        assertEquals(OrderStatus.PENDING, history.get(0).getStatus());
        assertEquals(ChangedByType.SYSTEM, history.get(0).getChangedByType());
        assertEquals("Order created", history.get(0).getNotes());

        ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(orderEventPublisherPort, times(1)).publishOrderCreated(captor.capture());
        OrderCreatedEvent event = captor.getValue();
        assertEquals(created.getId(), event.orderId());
        assertEquals(created.getOrderNumber(), event.orderNumber());
        assertEquals(created.getStatus(), event.status());
        assertEquals(0, new BigDecimal("200.00").compareTo(event.total()));
        assertEquals(CUSTOMER_KEYCLOAK_ID, event.customerId());
        assertNull(event.customerEmail());

        assertEquals(1, event.items().size());
        OrderCreatedEvent.OrderItemPayload item = event.items().get(0);
        assertEquals(VARIANT_ID, item.variantId());
        assertEquals(PRODUCT_ID, item.productId());
        assertEquals(2, item.quantity());
        assertEquals(SELLER_KEYCLOAK_ID, item.sellerKeycloakId());
        assertEquals(0, new BigDecimal("100.00").compareTo(item.priceSnapshot()));
        assertEquals(0, new BigDecimal("200.00").compareTo(item.subtotal()));
    }

    @Test
    void persistsGuestOrderWithoutCustomerId() {
        whenVariantAvailable();
        OrderDraft draft = new OrderDraft(
                null,
                " guest@example.com ",
                "guest@example.com",
                anOrderAddress(),
                List.of(new OrderDraft.OrderItemDraft(VARIANT_ID, 1)),
                null);

        Order created = createOrderUseCase.execute(draft);

        assertEquals("guest@example.com", created.getGuestEmail());
        assertNotNull(orderJpaRepository.findByOrderNumber(created.getOrderNumber()).orElseThrow());

        verify(orderEventPublisherPort, times(1))
                .publishOrderCreated(org.mockito.ArgumentMatchers.any(OrderCreatedEvent.class));
    }

    private void whenVariantAvailable() {
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
    }
}