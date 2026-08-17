package com.ekko.review_service.service;

import com.ekko.review_service.entity.EligibleReview;
import com.ekko.review_service.entity.Review;
import com.ekko.review_service.enums.ReviewStatus;
import com.ekko.review_service.exception.ReviewNotFoundException;
import com.ekko.review_service.mapper.ReviewMapper;
import com.ekko.review_service.repository.EligibleReviewRepository;
import com.ekko.review_service.repository.HelpfulVoteCountProjection;
import com.ekko.review_service.repository.RatingCountProjection;
import com.ekko.review_service.repository.ReviewHelpfulVoteRepository;
import com.ekko.review_service.repository.ReviewImageRepository;
import com.ekko.review_service.repository.ReviewRatingAggregateProjection;
import com.ekko.review_service.repository.ReviewRepository;
import com.ekko.review_service.dto.response.EligibleToReviewResponse;
import com.ekko.review_service.dto.response.ProductReviewsResponse;
import com.ekko.review_service.dto.response.ReviewResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ekko.review_service.support.ReviewTestDataBuilder.aReview;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewQueryServiceImplTest {

    private static final String CUSTOMER_ID = "customer-1";
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID REVIEW_ID = UUID.randomUUID();

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewImageRepository reviewImageRepository;

    @Mock
    private ReviewHelpfulVoteRepository reviewHelpfulVoteRepository;

    @Mock
    private EligibleReviewRepository eligibleReviewRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewQueryServiceImpl reviewQueryService;

    // ---------- getProductReviews ----------

    @Test
    @DisplayName("getProductReviews devuelve reviews visibles paginadas, promedio redondeado y distribución completa 1..5")
    void getProductReviews_shouldReturnVisibleReviewsAverageAndFullDistribution() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Review review1 = aReview().withId(UUID.randomUUID()).withProductId(PRODUCT_ID).build();
        Review review2 = aReview().withId(UUID.randomUUID()).withProductId(PRODUCT_ID).build();
        Page<Review> page = new PageImpl<>(List.of(review1, review2), pageable, 2);
        when(reviewRepository.findByProductIdAndStatus(PRODUCT_ID, ReviewStatus.VISIBLE, pageable))
                .thenReturn(page);
        when(reviewRepository.findRatingAggregateByProductId(PRODUCT_ID))
                .thenReturn(aggregate(new BigDecimal("4.50"), 2L));
        when(reviewRepository.findRatingDistributionByProductId(PRODUCT_ID))
                .thenReturn(List.of(distribution(5, 2L)));
        when(reviewHelpfulVoteRepository.countByReviewIdIn(List.of(review1.getId(), review2.getId())))
                .thenReturn(List.of());
        when(reviewImageRepository.findByReviewIdOrderBySortOrderAsc(any())).thenReturn(List.of());
        when(reviewMapper.toResponse(review1, List.of(), 0L)).thenReturn(response(review1, 0L));
        when(reviewMapper.toResponse(review2, List.of(), 0L)).thenReturn(response(review2, 0L));

        // when
        ProductReviewsResponse result = reviewQueryService.getProductReviews(PRODUCT_ID, pageable);

        // then
        assertThat(result.reviews()).hasSize(2);
        assertThat(result.averageRating()).isEqualByComparingTo(new BigDecimal("4.5"));
        assertThat(result.totalReviews()).isEqualTo(2L);
        assertThat(result.ratingDistribution()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of(
                1, 0L, 2, 0L, 3, 0L, 4, 0L, 5, 2L));
    }

    @Test
    @DisplayName("getProductReviews sin reviews devuelve promedio 0.0 y total 0")
    void getProductReviews_shouldReturnZeroAverageAndZeroTotalWhenNoReviews() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Page<Review> page = new PageImpl<>(List.of(), pageable, 0);
        when(reviewRepository.findByProductIdAndStatus(PRODUCT_ID, ReviewStatus.VISIBLE, pageable))
                .thenReturn(page);
        when(reviewRepository.findRatingAggregateByProductId(PRODUCT_ID)).thenReturn(aggregate(null, null));
        when(reviewRepository.findRatingDistributionByProductId(PRODUCT_ID)).thenReturn(List.of());

        // when
        ProductReviewsResponse result = reviewQueryService.getProductReviews(PRODUCT_ID, pageable);

        // then
        assertThat(result.reviews()).isEmpty();
        assertThat(result.averageRating()).isEqualByComparingTo(new BigDecimal("0.0"));
        assertThat(result.totalReviews()).isZero();
        assertThat(result.ratingDistribution()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of(
                1, 0L, 2, 0L, 3, 0L, 4, 0L, 5, 0L));
    }

    @Test
    @DisplayName("getProductReviews incluye el helpfulCount de cada review en el response")
    void getProductReviews_shouldIncludeHelpfulCountInEachReview() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Review review1 = aReview().withId(UUID.randomUUID()).withProductId(PRODUCT_ID).build();
        Review review2 = aReview().withId(UUID.randomUUID()).withProductId(PRODUCT_ID).build();
        Page<Review> page = new PageImpl<>(List.of(review1, review2), pageable, 2);
        when(reviewRepository.findByProductIdAndStatus(PRODUCT_ID, ReviewStatus.VISIBLE, pageable))
                .thenReturn(page);
        when(reviewRepository.findRatingAggregateByProductId(PRODUCT_ID)).thenReturn(aggregate(new BigDecimal("4.0"), 2L));
        when(reviewRepository.findRatingDistributionByProductId(PRODUCT_ID)).thenReturn(List.of());
        when(reviewHelpfulVoteRepository.countByReviewIdIn(List.of(review1.getId(), review2.getId())))
                .thenReturn(List.of(count(review1.getId(), 3L), count(review2.getId(), 5L)));
        when(reviewImageRepository.findByReviewIdOrderBySortOrderAsc(any())).thenReturn(List.of());
        when(reviewMapper.toResponse(review1, List.of(), 3L)).thenReturn(response(review1, 3L));
        when(reviewMapper.toResponse(review2, List.of(), 5L)).thenReturn(response(review2, 5L));

        // when
        ProductReviewsResponse result = reviewQueryService.getProductReviews(PRODUCT_ID, pageable);

        // then
        assertThat(result.reviews())
                .extracting(ReviewResponse::helpfulCount)
                .containsExactly(3L, 5L);
    }

    // ---------- getReviewById ----------

    @Test
    @DisplayName("getReviewById devuelve la review visible con su helpfulCount")
    void getReviewById_shouldReturnVisibleReviewWithHelpfulCount() {
        // given
        Review review = aReview().withId(REVIEW_ID).build();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewHelpfulVoteRepository.countByReviewId(REVIEW_ID)).thenReturn(7L);
        when(reviewImageRepository.findByReviewIdOrderBySortOrderAsc(REVIEW_ID)).thenReturn(List.of());
        when(reviewMapper.toResponse(review, List.of(), 7L)).thenReturn(response(review, 7L));

        // when
        ReviewResponse result = reviewQueryService.getReviewById(REVIEW_ID);

        // then
        assertThat(result.id()).isEqualTo(REVIEW_ID);
        assertThat(result.helpfulCount()).isEqualTo(7L);
    }

    @Test
    @DisplayName("getReviewById lanza ReviewNotFoundException cuando la review es HIDDEN")
    void getReviewById_shouldThrowReviewNotFoundWhenReviewIsHidden() {
        // given
        Review review = aReview().withId(REVIEW_ID).hidden().build();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        // when
        // then
        assertThatThrownBy(() -> reviewQueryService.getReviewById(REVIEW_ID))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    @DisplayName("getReviewById lanza ReviewNotFoundException cuando la review no existe")
    void getReviewById_shouldThrowReviewNotFoundWhenReviewDoesNotExist() {
        // given
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> reviewQueryService.getReviewById(REVIEW_ID))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    // ---------- getMyReviews ----------

    @Test
    @DisplayName("getMyReviews aplica sort por createdAt DESC cuando el pageable viene sin sort")
    void getMyReviews_shouldApplyDefaultSortByCreatedAtDescWhenPageableUnsorted() {
        // given
        Pageable unsorted = PageRequest.of(0, 20);
        Review review = aReview().withId(UUID.randomUUID()).build();
        Page<Review> page = new PageImpl<>(List.of(review), unsorted, 1);
        when(reviewRepository.findByCustomerId(eq(CUSTOMER_ID), any(Pageable.class))).thenReturn(page);
        when(reviewHelpfulVoteRepository.countByReviewIdIn(List.of(review.getId()))).thenReturn(List.of());
        when(reviewImageRepository.findByReviewIdOrderBySortOrderAsc(any())).thenReturn(List.of());
        when(reviewMapper.toResponse(review, List.of(), 0L)).thenReturn(response(review, 0L));

        // when
        reviewQueryService.getMyReviews(CUSTOMER_ID, unsorted);

        // then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewRepository).findByCustomerId(eq(CUSTOMER_ID), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().isSorted()).isTrue();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt"))
                .isNotNull()
                .extracting(org.springframework.data.domain.Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("getMyReviews respeta el sort del pageable cuando ya viene ordenado")
    void getMyReviews_shouldRespectProvidedSort() {
        // given
        Pageable sorted = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "rating"));
        Review review = aReview().withId(UUID.randomUUID()).build();
        Page<Review> page = new PageImpl<>(List.of(review), sorted, 1);
        when(reviewRepository.findByCustomerId(CUSTOMER_ID, sorted)).thenReturn(page);
        when(reviewHelpfulVoteRepository.countByReviewIdIn(List.of(review.getId()))).thenReturn(List.of());
        when(reviewImageRepository.findByReviewIdOrderBySortOrderAsc(any())).thenReturn(List.of());
        when(reviewMapper.toResponse(review, List.of(), 0L)).thenReturn(response(review, 0L));

        // when
        reviewQueryService.getMyReviews(CUSTOMER_ID, sorted);

        // then
        verify(reviewRepository).findByCustomerId(CUSTOMER_ID, sorted);
    }

    // ---------- getEligibleToReview ----------

    @Test
    @DisplayName("getEligibleToReview mapea los registros elegibles a la respuesta")
    void getEligibleToReview_shouldMapEligibleRecordsToResponse() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        EligibleReview eligible = EligibleReview.builder()
                .orderId(orderId)
                .orderItemId(orderItemId)
                .productId(productId)
                .customerId(CUSTOMER_ID)
                .build();
        when(eligibleReviewRepository.findEligibleWithoutReview(CUSTOMER_ID)).thenReturn(List.of(eligible));

        // when
        List<EligibleToReviewResponse> result = reviewQueryService.getEligibleToReview(CUSTOMER_ID);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(new EligibleToReviewResponse(orderId, orderItemId, productId));
    }

    @Test
    @DisplayName("getEligibleToReview devuelve lista vacía cuando no hay elegibles")
    void getEligibleToReview_shouldReturnEmptyListWhenNothingEligible() {
        // given
        when(eligibleReviewRepository.findEligibleWithoutReview(CUSTOMER_ID)).thenReturn(List.of());

        // when
        List<EligibleToReviewResponse> result = reviewQueryService.getEligibleToReview(CUSTOMER_ID);

        // then
        assertThat(result).isEmpty();
    }

    // ---------- adminGetAllReviews ----------

    @Test
    @DisplayName("adminGetAllReviews sin filtros consulta con Specification null")
    void adminGetAllReviews_shouldQueryWithNullSpecificationWhenNoFilters() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Review review = aReview().withId(UUID.randomUUID()).build();
        Page<Review> page = new PageImpl<>(List.of(review), pageable, 1);
        when(reviewRepository.findAll((Specification<Review>) isNull(), eq(pageable))).thenReturn(page);
        when(reviewHelpfulVoteRepository.countByReviewIdIn(List.of(review.getId()))).thenReturn(List.of());
        when(reviewImageRepository.findByReviewIdOrderBySortOrderAsc(any())).thenReturn(List.of());
        when(reviewMapper.toResponse(review, List.of(), 0L)).thenReturn(response(review, 0L));

        // when
        Page<ReviewResponse> result = reviewQueryService.adminGetAllReviews(null, null, null, pageable);

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("adminGetAllReviews combina los filtros de status, productId y customerId en una Specification")
    void adminGetAllReviews_shouldCombineStatusProductAndCustomerFilters() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Review review = aReview().withId(UUID.randomUUID()).build();
        Page<Review> page = new PageImpl<>(List.of(review), pageable, 1);
        when(reviewRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(reviewHelpfulVoteRepository.countByReviewIdIn(List.of(review.getId()))).thenReturn(List.of());
        when(reviewImageRepository.findByReviewIdOrderBySortOrderAsc(any())).thenReturn(List.of());
        when(reviewMapper.toResponse(review, List.of(), 0L)).thenReturn(response(review, 0L));

        // when
        Page<ReviewResponse> result = reviewQueryService.adminGetAllReviews(
                ReviewStatus.HIDDEN, PRODUCT_ID, CUSTOMER_ID, pageable);

        // then
        assertThat(result).hasSize(1);
        verify(reviewRepository).findAll(any(Specification.class), eq(pageable));
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

    private RatingCountProjection distribution(int rating, long count) {
        return new RatingCountProjection() {
            @Override
            public Integer getRating() {
                return rating;
            }

            @Override
            public Long getCount() {
                return count;
            }
        };
    }

    private HelpfulVoteCountProjection count(UUID reviewId, long count) {
        return new HelpfulVoteCountProjection() {
            @Override
            public UUID getReviewId() {
                return reviewId;
            }

            @Override
            public long getCount() {
                return count;
            }
        };
    }

    private ReviewResponse response(Review review, long helpfulCount) {
        return new ReviewResponse(
                review.getId(),
                review.getProductId(),
                review.getOrderId(),
                review.getOrderItemId(),
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                review.getStatus(),
                review.getIsVerifiedPurchase(),
                List.of(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                helpfulCount);
    }
}