package com.ekko.seller_service.messaging;

import com.ekko.seller_service.messaging.dto.consume.ProductDeactivatedEvent;
import com.ekko.seller_service.messaging.dto.consume.ProductPublishedEvent;
import com.ekko.seller_service.messaging.event.ProductStatusEventListener;
import com.ekko.seller_service.service.ProductStatusMetricsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductStatusEventListenerTest {

    @Mock
    private ProductStatusMetricsService metricsService;

    @InjectMocks
    private ProductStatusEventListener listener;

    @Test
    void onProductPublished_delegaAlServicio() {
        ProductPublishedEvent event = new ProductPublishedEvent(
                UUID.randomUUID(), UUID.randomUUID(), "Producto", "Electronica", null);

        listener.onProductPublished(event);

        verify(metricsService).applyPublished(event);
    }

    @Test
    void onProductDeactivated_delegaAlServicio() {
        ProductDeactivatedEvent event = new ProductDeactivatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), "ACTIVE", null);

        listener.onProductDeactivated(event);

        verify(metricsService).applyDeactivated(event);
    }
}