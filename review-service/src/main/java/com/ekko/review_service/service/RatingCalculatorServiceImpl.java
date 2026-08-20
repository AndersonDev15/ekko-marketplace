package com.ekko.review_service.service;

import com.ekko.review_service.messaging.ReviewEventPublisher;
import com.ekko.review_service.messaging.dto.publish.ProductRatingUpdatedEvent;
import com.ekko.review_service.repository.ReviewRatingAggregateProjection;
import com.ekko.review_service.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RatingCalculatorServiceImpl implements RatingCalculatorService {

    private final ReviewRepository reviewRepository;
    private final ReviewEventPublisher reviewEventPublisher;

    @Override
    public void recalculate(UUID productId) {
        ReviewRatingAggregateProjection aggregate = reviewRepository.findRatingAggregateByProductId(productId);

        BigDecimal averageRating = aggregate.getAverageRating() == null
                ? BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP)
                : aggregate.getAverageRating().setScale(1, RoundingMode.HALF_UP);
        long reviewCount = aggregate.getReviewCount() == null ? 0L : aggregate.getReviewCount();

        reviewEventPublisher.publishProductRatingUpdated(
                new ProductRatingUpdatedEvent(productId, averageRating, reviewCount));
    }
}