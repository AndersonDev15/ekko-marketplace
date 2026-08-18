package com.ekko.product_service.messaging;

import com.ekko.product_service.messaging.dto.OrderCancelledEvent;
import com.ekko.product_service.messaging.dto.OrderConfirmedEvent;
import com.ekko.product_service.service.OrderInventoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @Mock
    private OrderInventoryService inventoryService;

    @InjectMocks
    private OrderEventListener listener;

    @Test
    void onOrderConfirmed_delegaAlServicio() {
        OrderConfirmedEvent event = new OrderConfirmedEvent(
                UUID.randomUUID(), "EKK-001", "customer-1",
                List.of(new OrderConfirmedEvent.OrderItemConfirmed(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 2,
                        UUID.randomUUID(), new BigDecimal("100.00"))),
                null);

        listener.onOrderConfirmed(event);

        verify(inventoryService).confirmStock(event);
    }

    @Test
    void onOrderCancelled_delegaAlServicio() {
        OrderCancelledEvent event = new OrderCancelledEvent(
                UUID.randomUUID(), "EKK-001", UUID.randomUUID(), null, "CONFIRMED", true,
                List.of(new OrderCancelledEvent.OrderItemCancelled(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 2)),
                null);

        listener.onOrderCancelled(event);

        verify(inventoryService).releaseStock(event);
    }
}