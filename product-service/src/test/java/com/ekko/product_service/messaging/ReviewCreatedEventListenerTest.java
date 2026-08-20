package com.ekko.product_service.messaging;

import com.ekko.product_service.messaging.dto.consume.ReviewCreatedEvent;
import com.ekko.product_service.service.ProductRatingService;
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
    private ProductRatingService productRatingService;

    @InjectMocks
    private ReviewCreatedEventListener listener;

    @Test
    void onReviewCreated_delegaAlServicio() {
        ReviewCreatedEvent event = new ReviewCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "customer-1", 5, "Titulo", "Comentario", null);

        listener.onReviewCreated(event);

        verify(productRatingService).applyRating(event);
    }
}