package com.ekko.order_service.infrastructure.persistence.adapter.out;

import com.ekko.order_service.config.AbstractPostgresIntegrationTest;
import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.enums.OrderStatus;
import com.ekko.order_service.domain.port.out.OrderRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.ekko.order_service.builder.OrderTestDataBuilder.anOrderBuilder;
import static com.ekko.order_service.util.TestConstants.ADDRESS_CITY;
import static com.ekko.order_service.util.TestConstants.CUSTOMER_KEYCLOAK_ID;
import static com.ekko.order_service.util.TestConstants.PRODUCT_NAME;
import static com.ekko.order_service.util.TestConstants.VARIANT_ID;
import static com.ekko.order_service.util.TestConstants.VARIANT_SKU;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@Transactional
class OrderRepositoryAdapterIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private OrderRepositoryPort orderRepositoryPort;

    @Test
    void persistsAndRehydratesOrderWithItemsAndAddress() {
        Order order = anOrderBuilder().id(null).build();

        Order saved = orderRepositoryPort.save(order);

        assertNotNull(saved.getId());

        Optional<Order> found = orderRepositoryPort.findByOrderNumber(order.getOrderNumber());

        assertTrue(found.isPresent());
        Order rehydrated = found.get();
        assertEquals(order.getOrderNumber(), rehydrated.getOrderNumber());
        assertEquals(order.getCustomerId(), rehydrated.getCustomerId());
        assertEquals(OrderStatus.PENDING, rehydrated.getStatus());
        assertEquals(order.getTotal(), rehydrated.getTotal());
        assertNotNull(rehydrated.getAddress());
        assertEquals(ADDRESS_CITY, rehydrated.getAddress().city());
        assertEquals(order.getItems().size(), rehydrated.getItems().size());
        assertEquals(VARIANT_ID, rehydrated.getItems().get(0).variantId());
        assertEquals(VARIANT_SKU, rehydrated.getItems().get(0).variantSnapshot());
        assertEquals(PRODUCT_NAME, rehydrated.getItems().get(0).productNameSnapshot());
    }

    @Test
    void findsOrdersByCustomerIdPaginated() {
        Order order = anOrderBuilder().id(null).build();
        orderRepositoryPort.save(order);

        Page<Order> page = orderRepositoryPort.findAllByCustomerId(
                CUSTOMER_KEYCLOAK_ID, PageRequest.of(0, 10));

        assertTrue(page.getTotalElements() >= 1);
        assertEquals(CUSTOMER_KEYCLOAK_ID, page.getContent().get(0).getCustomerId());
    }

    @Test
    void findsAllOrdersPaginated() {
        orderRepositoryPort.save(anOrderBuilder().id(null).build());

        Page<Order> page = orderRepositoryPort.findAll(PageRequest.of(0, 10));

        assertTrue(page.getTotalElements() >= 1);
    }

    @Test
    void returnsEmptyWhenOrderNumberMissing() {
        Optional<Order> found = orderRepositoryPort.findByOrderNumber("EKK-99999999-ZZZZ");

        assertTrue(found.isEmpty());
    }
}