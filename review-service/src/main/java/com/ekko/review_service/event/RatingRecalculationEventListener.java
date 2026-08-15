package com.ekko.review_service.event;

import com.ekko.review_service.service.RatingCalculatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RatingRecalculationEventListener {

    private final RatingCalculatorService ratingCalculatorService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRatingRecalculationRequested(RatingRecalculationRequestedEvent event) {
        ratingCalculatorService.recalculate(event.productId());
    }
}