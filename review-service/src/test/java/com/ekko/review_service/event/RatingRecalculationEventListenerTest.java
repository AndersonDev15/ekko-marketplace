package com.ekko.review_service.event;

import com.ekko.review_service.service.RatingCalculatorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RatingRecalculationEventListenerTest {

    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @Mock
    private RatingCalculatorService ratingCalculatorService;

    @InjectMocks
    private RatingRecalculationEventListener listener;

    @Test
    @DisplayName("on delega a RatingCalculatorService.recalculate con el productId del evento")
    void on_shouldDelegateToRatingCalculatorServiceWithEventProductId() {
        // given
        RatingRecalculationRequestedEvent event = new RatingRecalculationRequestedEvent(PRODUCT_ID);

        // when
        listener.onRatingRecalculationRequested(event);

        // then
        verify(ratingCalculatorService).recalculate(PRODUCT_ID);
    }
}