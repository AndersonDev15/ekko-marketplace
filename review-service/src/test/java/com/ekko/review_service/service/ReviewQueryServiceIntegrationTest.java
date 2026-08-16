package com.ekko.review_service.service;

import com.ekko.review_service.config.AbstractPostgresIntegrationTest;
import com.ekko.review_service.entity.Review;
import com.ekko.review_service.enums.ReviewStatus;
import com.ekko.review_service.exception.ReviewNotFoundException;
import com.ekko.review_service.repository.ReviewRepository;
import com.ekko.review_service.web.dto.EligibleToReviewResponse;
import com.ekko.review_service.web.dto.ProductReviewsResponse;
import com.ekko.review_service.web.dto.ReviewResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.ekko.review_service.support.ReviewTestDataBuilder.aReview;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewQueryServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String CUSTOMER_ID = "customer-1";
    private static final String ANOTHER_CUSTOMER_ID = "customer-2";

    @Autowired
    private ReviewQueryService reviewQueryService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Test
    @DisplayName("getProductReviews devuelve solo reviews VISIBLE con promedio, total y distribución de la BD real")
    void getProductReviews_shouldReturnOnlyVisibleReviewsWithAggregates() {
        // given
        UUID productId = UUID.randomUUID();
        reviewRepository.save(aReview().withProductId(productId).withRating(5).build());
        reviewRepository.save(aReview().withProductId(productId).withRating(3).build());
        reviewRepository.save(aReview().withProductId(productId).withRating(1).hidden().build());

        // when
        ProductReviewsResponse result = reviewQueryService.getProductReviews(productId, PageRequest.of(0, 20));

        // then
        assertThat(result.reviews()).hasSize(2);
        assertThat(result.averageRating()).isEqualByComparingTo(new BigDecimal("4.0"));
        assertThat(result.totalReviews()).isEqualTo(2L);
        assertThat(result.ratingDistribution()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of(
                1, 0L, 2, 0L, 3, 1L, 4, 0L, 5, 1L));
    }

    @Test
    @DisplayName("getProductReviews incluye el helpfulCount real de cada review")
    void getProductReviews_shouldIncludeRealHelpfulCount() {
        // given
        UUID productId = UUID.randomUUID();
        UUID reviewId = reviewRepository.save(aReview().withProductId(productId).withRating(5).build()).getId();
        jdbcTemplate.update("INSERT INTO review_helpful_votes (review_id, customer_id) VALUES (?, ?)",
                reviewId, "voter-1");
        jdbcTemplate.update("INSERT INTO review_helpful_votes (review_id, customer_id) VALUES (?, ?)",
                reviewId, "voter-2");

        // when
        ProductReviewsResponse result = reviewQueryService.getProductReviews(productId, PageRequest.of(0, 20));

        // then
        assertThat(result.reviews().getContent()).hasSize(1);
        assertThat(result.reviews().getContent().get(0).helpfulCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getReviewById devuelve una review visible")
    void getReviewById_shouldReturnVisibleReview() {
        // given
        Review review = reviewRepository.save(aReview().build());

        // when
        ReviewResponse result = reviewQueryService.getReviewById(review.getId());

        // then
        assertThat(result.id()).isEqualTo(review.getId());
    }

    @Test
    @DisplayName("getReviewById lanza ReviewNotFoundException para una review HIDDEN")
    void getReviewById_shouldThrowReviewNotFoundForHiddenReview() {
        // given
        Review review = reviewRepository.save(aReview().hidden().build());

        // when
        // then
        assertThatThrownBy(() -> reviewQueryService.getReviewById(review.getId()))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    @DisplayName("getMyReviews devuelve solo las reviews del customer")
    void getMyReviews_shouldReturnOnlyOwnReviews() {
        // given
        UUID productId = UUID.randomUUID();
        UUID own1 = reviewRepository.save(
                aReview().withProductId(productId).withCustomerId(CUSTOMER_ID).build()).getId();
        UUID own2 = reviewRepository.save(
                aReview().withProductId(productId).withCustomerId(CUSTOMER_ID).build()).getId();
        reviewRepository.save(aReview().withProductId(productId).withCustomerId(ANOTHER_CUSTOMER_ID).build());

        // when
        Page<ReviewResponse> result = reviewQueryService.getMyReviews(CUSTOMER_ID, PageRequest.of(0, 20));

        // then
        assertThat(result.getTotalElements()).isEqualTo(2L);
        assertThat(result.getContent())
                .extracting(ReviewResponse::id)
                .containsExactlyInAnyOrder(own1, own2);
    }

    @Test
    @DisplayName("getMyReviews ordena por createdAt DESC por defecto")
    void getMyReviews_shouldSortByCreatedAtDescByDefault() {
        // given
        UUID productId = UUID.randomUUID();
        reviewRepository.save(aReview().withProductId(productId).withCustomerId(CUSTOMER_ID).withRating(5).build());
        reviewRepository.save(aReview().withProductId(productId).withCustomerId(CUSTOMER_ID).withRating(3).build());

        // when
        Page<ReviewResponse> result = reviewQueryService.getMyReviews(CUSTOMER_ID, PageRequest.of(0, 20));

        // then
        List<LocalDateTime> createdAt = result.getContent().stream()
                .map(ReviewResponse::createdAt)
                .toList();
        assertThat(createdAt).isSortedAccordingTo(java.util.Comparator.reverseOrder());
    }

    @Test
    @DisplayName("getEligibleToReview excluye los order items ya reviewados")
    void getEligibleToReview_shouldExcludeAlreadyReviewedItems() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID reviewedItem = UUID.randomUUID();
        UUID pendingItem = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        insertEligibility(orderId, reviewedItem, productId);
        insertEligibility(orderId, pendingItem, productId);
        reviewRepository.save(aReview().withCustomerId(CUSTOMER_ID).withOrderItemId(reviewedItem).build());

        // when
        List<EligibleToReviewResponse> result = reviewQueryService.getEligibleToReview(CUSTOMER_ID);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).orderItemId()).isEqualTo(pendingItem);
    }

    @Test
    @DisplayName("getEligibleToReview devuelve lista vacía cuando no quedan elegibles")
    void getEligibleToReview_shouldReturnEmptyWhenNothingLeft() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID reviewedItem = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        insertEligibility(orderId, reviewedItem, productId);
        reviewRepository.save(aReview().withCustomerId(CUSTOMER_ID).withOrderItemId(reviewedItem).build());

        // when
        List<EligibleToReviewResponse> result = reviewQueryService.getEligibleToReview(CUSTOMER_ID);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("adminGetAllReviews filtra por status")
    void adminGetAllReviews_shouldFilterByStatus() {
        // given
        UUID productId = UUID.randomUUID();
        reviewRepository.save(aReview().withProductId(productId).build());
        reviewRepository.save(aReview().withProductId(productId).hidden().build());

        // when
        Page<ReviewResponse> hidden = reviewQueryService.adminGetAllReviews(
                ReviewStatus.HIDDEN, null, null, PageRequest.of(0, 20));

        // then
        assertThat(hidden.getTotalElements()).isEqualTo(1L);
        assertThat(hidden.getContent().get(0).status()).isEqualTo(ReviewStatus.HIDDEN);
    }

    @Test
    @DisplayName("adminGetAllReviews filtra por productId")
    void adminGetAllReviews_shouldFilterByProductId() {
        // given
        UUID productId = UUID.randomUUID();
        UUID otherProductId = UUID.randomUUID();
        reviewRepository.save(aReview().withProductId(productId).build());
        reviewRepository.save(aReview().withProductId(productId).build());
        reviewRepository.save(aReview().withProductId(otherProductId).build());

        // when
        Page<ReviewResponse> result = reviewQueryService.adminGetAllReviews(
                null, productId, null, PageRequest.of(0, 20));

        // then
        assertThat(result.getTotalElements()).isEqualTo(2L);
        assertThat(result.getContent())
                .extracting(ReviewResponse::productId)
                .containsOnly(productId);
    }

    @Test
    @DisplayName("adminGetAllReviews filtra por customerId")
    void adminGetAllReviews_shouldFilterByCustomerId() {
        // given
        UUID productId = UUID.randomUUID();
        UUID ownId = reviewRepository.save(
                aReview().withProductId(productId).withCustomerId(CUSTOMER_ID).build()).getId();
        reviewRepository.save(aReview().withProductId(productId).withCustomerId(ANOTHER_CUSTOMER_ID).build());

        // when
        Page<ReviewResponse> result = reviewQueryService.adminGetAllReviews(
                null, null, CUSTOMER_ID, PageRequest.of(0, 20));

        // then
        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getContent().get(0).id()).isEqualTo(ownId);
    }

    @Test
    @DisplayName("adminGetAllReviews sin filtros devuelve todas las reviews paginadas")
    void adminGetAllReviews_shouldReturnAllReviewsWithoutFilters() {
        // given
        UUID productId = UUID.randomUUID();
        reviewRepository.save(aReview().withProductId(productId).build());
        reviewRepository.save(aReview().withProductId(productId).hidden().build());

        // when
        Page<ReviewResponse> result = reviewQueryService.adminGetAllReviews(
                null, null, null, PageRequest.of(0, 1, Sort.by("createdAt")));

        // then
        assertThat(result.getTotalElements()).isEqualTo(2L);
        assertThat(result.getContent()).hasSize(1);
    }

    private void insertEligibility(UUID orderId, UUID orderItemId, UUID productId) {
        jdbcTemplate.update("""
                INSERT INTO eligible_reviews (order_id, order_item_id, product_id, customer_id)
                VALUES (?, ?, ?, ?)
                """, orderId, orderItemId, productId, CUSTOMER_ID);
    }
}