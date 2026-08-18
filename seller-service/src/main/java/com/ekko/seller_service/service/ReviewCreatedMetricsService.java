package com.ekko.seller_service.service;

import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerMetrics;
import com.ekko.seller_service.messaging.dto.ReviewCreatedEvent;
import com.ekko.seller_service.repository.ReviewConfirmationRepository;
import com.ekko.seller_service.repository.SellerMetricsRepository;
import com.ekko.seller_service.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewCreatedMetricsService {

    private static final int RATING_SCALE = 2;

    private final ReviewConfirmationRepository confirmationRepository;
    private final SellerRepository sellerRepository;
    private final SellerMetricsRepository metricsRepository;

    /**
     * Applies the metrics of a created review: totalReviews is incremented by 1 and
     * averageRating is recomputed incrementally as
     * (averageRating * oldTotalReviews + newRating) / (oldTotalReviews + 1).
     * The review is marked as processed atomically, so redeliveries do not
     * double-count reviews nor corrupt the average.
     */
    @Transactional
    public void applyMetrics(ReviewCreatedEvent event) {
        if (confirmationRepository.insertIfAbsent(event.reviewId()) == 0) {
            return;
        }

        sellerRepository.findByKeycloakId(event.sellerKeycloakId().toString())
                .ifPresentOrElse(
                        seller -> updateMetrics(seller, event.rating()),
                        () -> log.warn("review.created {} references unknown seller {}; metrics skipped",
                                event.reviewId(), event.sellerKeycloakId()));
    }

    private void updateMetrics(Seller seller, Integer rating) {
        SellerMetrics metrics = metricsRepository.findBySellerId(seller.getId())
                .orElseGet(() -> SellerMetrics.builder().seller(seller).build());

        long oldCount = metrics.getTotalReviews();
        BigDecimal newAverage = metrics.getAverageRating()
                .multiply(BigDecimal.valueOf(oldCount))
                .add(BigDecimal.valueOf(rating))
                .divide(BigDecimal.valueOf(oldCount + 1), RATING_SCALE, RoundingMode.HALF_UP);

        metrics.setTotalReviews(oldCount + 1);
        metrics.setAverageRating(newAverage);
        metricsRepository.save(metrics);
    }
}