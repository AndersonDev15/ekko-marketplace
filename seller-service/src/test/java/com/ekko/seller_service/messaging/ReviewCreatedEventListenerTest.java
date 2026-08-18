package com.ekko.seller_service.messaging;

import com.ekko.seller_service.messaging.dto.ReviewCreatedEvent;
import com.ekko.seller_service.service.ReviewCreatedMetricsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReviewCreatedEventListenerTest {

    @Mock
    private ReviewCreatedMetricsService metricsService;

    @InjectMocks
    private ReviewCreatedEventListener listener;

    @Test
    void onReviewCreated_delegaAlServicio() {
        ReviewCreatedEvent event = event(5);

        listener.onReviewCreated(event);

        verify(metricsService).applyMetrics(event);
    }

    private static ReviewCreatedEvent event(int rating) {
        return new ReviewCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "customer-1", rating, "Titulo", "Comentario", null);
    }
}