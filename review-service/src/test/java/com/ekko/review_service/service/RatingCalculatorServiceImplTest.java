package com.ekko.review_service.service;

import com.ekko.review_service.messaging.ReviewEventPublisher;
import com.ekko.review_service.messaging.dto.publish.ProductRatingUpdatedEvent;
import com.ekko.review_service.repository.ReviewRatingAggregateProjection;
import com.ekko.review_service.repository.ReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatingCalculatorServiceImplTest {

    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewEventPublisher reviewEventPublisher;

    @InjectMocks
    private RatingCalculatorServiceImpl ratingCalculatorService;

    @Test
    @DisplayName("recalculate con COUNT > 0 calcula el promedio redondeado a 1 decimal HALF_UP y publica el evento correcto")
    void recalculate_shouldPublishEventWithRoundedAverageAndCountWhenCountGreaterThanZero() {
        // given
        ReviewRatingAggregateProjection aggregate = aggregate(new BigDecimal("4.25"), 4L);
        when(reviewRepository.findRatingAggregateByProductId(PRODUCT_ID)).thenReturn(aggregate);

        // when
        ratingCalculatorService.recalculate(PRODUCT_ID);

        // then
        ArgumentCaptor<ProductRatingUpdatedEvent> captor = ArgumentCaptor.forClass(ProductRatingUpdatedEvent.class);
        verify(reviewEventPublisher).publishProductRatingUpdated(captor.capture());
        ProductRatingUpdatedEvent event = captor.getValue();
        assertThat(event.productId()).isEqualTo(PRODUCT_ID);
        assertThat(event.averageRating()).isEqualByComparingTo(new BigDecimal("4.3"));
        assertThat(event.reviewCount()).isEqualTo(4L);
    }

    @Test
    @DisplayName("recalculate con COUNT = 0 publica averageRating 0.0 y reviewCount 0, pero igual publica el evento")
    void recalculate_shouldPublishZeroAverageAndZeroCountWhenNoReviews() {
        // given
        ReviewRatingAggregateProjection aggregate = aggregate(null, null);
        when(reviewRepository.findRatingAggregateByProductId(PRODUCT_ID)).thenReturn(aggregate);

        // when
        ratingCalculatorService.recalculate(PRODUCT_ID);

        // then
        ArgumentCaptor<ProductRatingUpdatedEvent> captor = ArgumentCaptor.forClass(ProductRatingUpdatedEvent.class);
        verify(reviewEventPublisher).publishProductRatingUpdated(captor.capture());
        ProductRatingUpdatedEvent event = captor.getValue();
        assertThat(event.productId()).isEqualTo(PRODUCT_ID);
        assertThat(event.averageRating()).isEqualByComparingTo(BigDecimal.ZERO.setScale(1));
        assertThat(event.reviewCount()).isZero();
    }

    @ParameterizedTest
    @MethodSource("roundingCases")
    @DisplayName("recalculate redondea el promedio con HALF_UP a 1 decimal")
    void recalculate_shouldRoundAverageHalfUp(String rawAverage, String expectedAverage) {
        // given
        ReviewRatingAggregateProjection aggregate = aggregate(new BigDecimal(rawAverage), 2L);
        when(reviewRepository.findRatingAggregateByProductId(PRODUCT_ID)).thenReturn(aggregate);

        // when
        ratingCalculatorService.recalculate(PRODUCT_ID);

        // then
        ArgumentCaptor<ProductRatingUpdatedEvent> captor = ArgumentCaptor.forClass(ProductRatingUpdatedEvent.class);
        verify(reviewEventPublisher).publishProductRatingUpdated(captor.capture());
        assertThat(captor.getValue().averageRating())
                .isEqualByComparingTo(new BigDecimal(expectedAverage));
    }

    @Test
    @DisplayName("recalculate publica exactamente una vez el evento de rating actualizado")
    void recalculate_shouldPublishEventExactlyOnce() {
        // given
        ReviewRatingAggregateProjection aggregate = aggregate(new BigDecimal("4.0"), 2L);
        when(reviewRepository.findRatingAggregateByProductId(PRODUCT_ID)).thenReturn(aggregate);

        // when
        ratingCalculatorService.recalculate(PRODUCT_ID);

        // then
        verify(reviewEventPublisher).publishProductRatingUpdated(
                new ProductRatingUpdatedEvent(PRODUCT_ID, new BigDecimal("4.0"), 2L));
    }

    private static Stream<Arguments> roundingCases() {
        return Stream.of(
                Arguments.of("4.05", "4.1"),
                Arguments.of("4.04", "4.0"));
    }

    private ReviewRatingAggregateProjection aggregate(BigDecimal averageRating, Long reviewCount) {
        return new ReviewRatingAggregateProjection() {
            @Override
            public BigDecimal getAverageRating() {
                return averageRating;
            }

            @Override
            public Long getReviewCount() {
                return reviewCount;
            }
        };
    }
}