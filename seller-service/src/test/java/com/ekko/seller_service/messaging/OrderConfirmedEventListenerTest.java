package com.ekko.seller_service.messaging;

import com.ekko.seller_service.messaging.dto.consume.OrderConfirmedEvent;
import com.ekko.seller_service.service.OrderConfirmedMetricsService;
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
class OrderConfirmedEventListenerTest {

    @Mock
    private OrderConfirmedMetricsService metricsService;

    @InjectMocks
    private OrderConfirmedEventListener listener;

    @Test
    void onOrderConfirmed_delegaAlServicio() {
        OrderConfirmedEvent event = new OrderConfirmedEvent(
                UUID.randomUUID(), "ORD-001", UUID.randomUUID(), "guest@ekko.test",
                List.of(new OrderConfirmedEvent.OrderItemConfirmed(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN)),
                null);

        listener.onOrderConfirmed(event);

        verify(metricsService).applyMetrics(event);
    }
}