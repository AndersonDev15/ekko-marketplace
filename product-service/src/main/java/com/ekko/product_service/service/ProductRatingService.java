package com.ekko.product_service.service;

import com.ekko.product_service.entity.Product;
import com.ekko.product_service.messaging.dto.consume.ReviewCreatedEvent;
import com.ekko.product_service.repository.ProductRepository;
import com.ekko.product_service.repository.ReviewEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductRatingService {

    private static final int RATING_SCALE = 1;

    private final ReviewEventRepository reviewEventRepository;
    private final ProductRepository productRepository;

    /**
     * Updates the product's stored average rating and review count using the new
     * rating, incrementally: (averageRating * reviewCount + newRating) / (reviewCount + 1).
     * The product row is locked pessimistically so concurrent reviews do not lose
     * updates. The review is marked as processed atomically, so redeliveries do
     * not count twice.
     */
    @Transactional
    public void applyRating(ReviewCreatedEvent event) {
        if (reviewEventRepository.insertIfAbsent(event.reviewId()) == 0) {
            return;
        }

        productRepository.findByIdForUpdate(event.productId())
                .ifPresentOrElse(
                        product -> updateRating(product, event.rating()),
                        () -> log.warn("review.created {} references unknown product {}; rating skipped",
                                event.reviewId(), event.productId()));
    }

    private void updateRating(Product product, Integer rating) {
        int count = product.getReviewCount() != null ? product.getReviewCount() : 0;
        BigDecimal average = product.getAverageRating() != null ? product.getAverageRating() : BigDecimal.ZERO;

        BigDecimal newAverage = average
                .multiply(BigDecimal.valueOf(count))
                .add(BigDecimal.valueOf(rating))
                .divide(BigDecimal.valueOf(count + 1), RATING_SCALE, RoundingMode.HALF_UP);

        product.setReviewCount(count + 1);
        product.setAverageRating(newAverage);
        productRepository.save(product);
    }
}