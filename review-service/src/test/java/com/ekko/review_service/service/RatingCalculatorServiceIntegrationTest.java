package com.ekko.review_service.service;

import com.ekko.review_service.config.AbstractPostgresIntegrationTest;
import com.ekko.review_service.messaging.dto.publish.ProductRatingUpdatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static com.ekko.review_service.support.ReviewTestDataBuilder.aReview;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RatingCalculatorServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private RatingCalculatorService ratingCalculatorService;

    @Autowired
    private com.ekko.review_service.repository.ReviewRepository reviewRepository;

    @Test
    @DisplayName("recalculate calcula el promedio real solo con reviews VISIBLE y publica el evento")
    void recalculate_shouldComputeAverageOnlyFromVisibleReviews() {
        // given
        UUID productId = UUID.randomUUID();
        reviewRepository.save(aReview().withProductId(productId).withRating(5).build());
        reviewRepository.save(aReview().withProductId(productId).withRating(4).build());
        reviewRepository.save(aReview().withProductId(productId).withRating(1).hidden().build());

        // when
        ratingCalculatorService.recalculate(productId);

        // then
        ArgumentCaptor<ProductRatingUpdatedEvent> captor = ArgumentCaptor.forClass(ProductRatingUpdatedEvent.class);
        verify(reviewEventPublisher).publishProductRatingUpdated(captor.capture());
        assertThat(captor.getValue().productId()).isEqualTo(productId);
        assertThat(captor.getValue().averageRating()).isEqualByComparingTo(new BigDecimal("4.5"));
        assertThat(captor.getValue().reviewCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("recalculate redondea el promedio a 1 decimal con HALF_UP")
    void recalculate_shouldRoundAverageHalfUp() {
        // given
        UUID productId = UUID.randomUUID();
        reviewRepository.save(aReview().withProductId(productId).withRating(5).build());
        reviewRepository.save(aReview().withProductId(productId).withRating(4).build());
        reviewRepository.save(aReview().withProductId(productId).withRating(4).build());

        // when
        ratingCalculatorService.recalculate(productId);

        // then
        ArgumentCaptor<ProductRatingUpdatedEvent> captor = ArgumentCaptor.forClass(ProductRatingUpdatedEvent.class);
        verify(reviewEventPublisher).publishProductRatingUpdated(captor.capture());
        assertThat(captor.getValue().averageRating()).isEqualByComparingTo(new BigDecimal("4.3"));
    }

    @Test
    @DisplayName("recalculate sin reviews publica promedio 0.0 y contador 0")
    void recalculate_shouldPublishZeroAverageAndZeroCountWhenNoReviews() {
        // given
        UUID productId = UUID.randomUUID();

        // when
        ratingCalculatorService.recalculate(productId);

        // then
        ArgumentCaptor<ProductRatingUpdatedEvent> captor = ArgumentCaptor.forClass(ProductRatingUpdatedEvent.class);
        verify(reviewEventPublisher).publishProductRatingUpdated(captor.capture());
        assertThat(captor.getValue().averageRating()).isEqualByComparingTo(new BigDecimal("0.0"));
        assertThat(captor.getValue().reviewCount()).isZero();
    }

    @Test
    @DisplayName("recalculate no cuenta las reviews de otros productos")
    void recalculate_shouldIgnoreReviewsFromOtherProducts() {
        // given
        UUID productId = UUID.randomUUID();
        UUID otherProductId = UUID.randomUUID();
        reviewRepository.save(aReview().withProductId(otherProductId).withRating(5).build());

        // when
        ratingCalculatorService.recalculate(productId);

        // then
        ArgumentCaptor<ProductRatingUpdatedEvent> captor = ArgumentCaptor.forClass(ProductRatingUpdatedEvent.class);
        verify(reviewEventPublisher).publishProductRatingUpdated(captor.capture());
        assertThat(captor.getValue().averageRating()).isEqualByComparingTo(new BigDecimal("0.0"));
        assertThat(captor.getValue().reviewCount()).isZero();
    }

    @Test
    @DisplayName("recalculate con solo reviews HIDDEN se comporta como si no hubiera reviews")
    void recalculate_shouldTreatOnlyHiddenReviewsAsNoReviews() {
        // given
        UUID productId = UUID.randomUUID();
        reviewRepository.save(aReview().withProductId(productId).withRating(5).hidden().build());

        // when
        ratingCalculatorService.recalculate(productId);

        // then
        verify(reviewEventPublisher, times(1)).publishProductRatingUpdated(
                new ProductRatingUpdatedEvent(productId, new BigDecimal("0.0"), 0L));
    }
}