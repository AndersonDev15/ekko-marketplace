package com.ekko.review_service.service;

import com.ekko.review_service.config.AbstractPostgresIntegrationTest;
import com.ekko.review_service.enums.ReviewStatus;
import com.ekko.review_service.exception.NotEligibleToReviewException;
import com.ekko.review_service.exception.ReviewAlreadyExistsException;
import com.ekko.review_service.exception.ReviewNotFoundException;
import com.ekko.review_service.messaging.dto.ProductRatingUpdatedEvent;
import com.ekko.review_service.messaging.dto.ReviewCreatedEvent;
import com.ekko.review_service.repository.EligibleReviewRepository;
import com.ekko.review_service.repository.ReviewImageRepository;
import com.ekko.review_service.repository.ReviewRepository;
import com.ekko.review_service.dto.request.AdminUpdateContentRequest;
import com.ekko.review_service.dto.request.CreateReviewRequest;
import com.ekko.review_service.dto.response.ReviewResponse;
import com.ekko.review_service.dto.request.UpdateReviewRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

class ReviewCommandServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String CUSTOMER_ID = "customer-1";
    private static final String ANOTHER_CUSTOMER_ID = "customer-2";
    private static final String ADMIN_ID = "admin-1";
    private static final UUID SELLER_KEYCLOAK_ID = UUID.randomUUID();

    @Autowired
    private ReviewCommandService reviewCommandService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewImageRepository reviewImageRepository;

    @Autowired
    private EligibleReviewRepository eligibleReviewRepository;

    // ---------- createReview ----------

    @Test
    @DisplayName("createReview persiste review + imágenes con sortOrder y dispara el recálculo real del rating")
    void createReview_shouldPersistReviewWithImagesAndTriggerRecalculation() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        insertEligibility(orderId, orderItemId, productId);
        CreateReviewRequest request = new CreateReviewRequest(
                productId, orderId, orderItemId, 5, "Great", "Nice", List.of("url-1", "url-2"));

        // when
        ReviewResponse response = reviewCommandService.createReview(request, CUSTOMER_ID);

        // then
        assertThat(response.id()).isNotNull();
        assertThat(response.status()).isEqualTo(ReviewStatus.VISIBLE);
        assertThat(response.isVerifiedPurchase()).isTrue();
        assertThat(response.imageUrls()).containsExactly("url-1", "url-2");
        Long images = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM review_images WHERE review_id = ?", Long.class, response.id());
        assertThat(images).isEqualTo(2L);
        List<Integer> sortOrders = jdbcTemplate.queryForList(
                "SELECT sort_order FROM review_images WHERE review_id = ? ORDER BY sort_order",
                Integer.class, response.id());
        assertThat(sortOrders).containsExactly(0, 1);

        ArgumentCaptor<ProductRatingUpdatedEvent> captor = ArgumentCaptor.forClass(ProductRatingUpdatedEvent.class);
        verify(reviewEventPublisher).publishProductRatingUpdated(captor.capture());
        assertThat(captor.getValue().productId()).isEqualTo(productId);
        assertThat(captor.getValue().averageRating()).isEqualByComparingTo(new BigDecimal("5.0"));
        assertThat(captor.getValue().reviewCount()).isEqualTo(1L);

        ArgumentCaptor<ReviewCreatedEvent> createdCaptor = ArgumentCaptor.forClass(ReviewCreatedEvent.class);
        verify(reviewEventPublisher).publishReviewCreated(createdCaptor.capture());
        ReviewCreatedEvent created = createdCaptor.getValue();
        assertThat(created.reviewId()).isEqualTo(response.id());
        assertThat(created.productId()).isEqualTo(productId);
        assertThat(created.orderId()).isEqualTo(orderId);
        assertThat(created.orderItemId()).isEqualTo(orderItemId);
        assertThat(created.sellerKeycloakId()).isEqualTo(SELLER_KEYCLOAK_ID);
        assertThat(created.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(created.rating()).isEqualTo(5);
        assertThat(created.title()).isEqualTo("Great");
        assertThat(created.comment()).isEqualTo("Nice");
        assertThat(created.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("createReview lanza NotEligibleToReviewException cuando el customer no es elegible")
    void createReview_shouldThrowWhenCustomerNotEligible() {
        // given
        CreateReviewRequest request = new CreateReviewRequest(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 5, "Great", "Nice", List.of());

        // when
        // then
        assertThatThrownBy(() -> reviewCommandService.createReview(request, CUSTOMER_ID))
                .isInstanceOf(NotEligibleToReviewException.class);
        assertThat(reviewRepository.count()).isZero();
    }

    @Test
    @DisplayName("createReview lanza ReviewAlreadyExistsException cuando ya existe review del orderItem para el customer")
    void createReview_shouldThrowAlreadyExistsWhenOrderItemAlreadyReviewed() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        insertEligibility(orderId, orderItemId, productId);
        CreateReviewRequest first = new CreateReviewRequest(
                productId, orderId, orderItemId, 5, "Great", "Nice", List.of());
        CreateReviewRequest duplicate = new CreateReviewRequest(
                productId, orderId, orderItemId, 4, "Again", "Again", List.of());
        reviewCommandService.createReview(first, CUSTOMER_ID);

        // when
        // then
        assertThatThrownBy(() -> reviewCommandService.createReview(duplicate, CUSTOMER_ID))
                .isInstanceOf(ReviewAlreadyExistsException.class);
        assertThat(reviewRepository.count()).isEqualTo(1L);
    }

    // ---------- updateReview ----------

    @Test
    @DisplayName("updateReview actualiza los campos y reemplaza las imágenes cuando vienen en el request")
    void updateReview_shouldUpdateFieldsAndReplaceImages() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        insertEligibility(orderId, orderItemId, productId);
        ReviewResponse created = reviewCommandService.createReview(
                new CreateReviewRequest(productId, orderId, orderItemId, 5, "Great", "Nice", List.of("old-url")),
                CUSTOMER_ID);
        reset(reviewEventPublisher);
        UpdateReviewRequest request = new UpdateReviewRequest(4, "New title", "New comment", List.of("new-url"));

        // when
        ReviewResponse updated = reviewCommandService.updateReview(created.id(), request, CUSTOMER_ID);

        // then
        assertThat(updated.rating()).isEqualTo(4);
        assertThat(updated.title()).isEqualTo("New title");
        assertThat(updated.comment()).isEqualTo("New comment");
        assertThat(updated.imageUrls()).containsExactly("new-url");
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM review_images WHERE review_id = ?", Long.class, created.id());
        assertThat(count).isEqualTo(1L);

        ArgumentCaptor<ProductRatingUpdatedEvent> captor = ArgumentCaptor.forClass(ProductRatingUpdatedEvent.class);
        verify(reviewEventPublisher).publishProductRatingUpdated(captor.capture());
        assertThat(captor.getValue().averageRating()).isEqualByComparingTo(new BigDecimal("4.0"));
    }

    @Test
    @DisplayName("updateReview no publica recálculo cuando el rating no cambia")
    void updateReview_shouldNotTriggerRecalculationWhenRatingUnchanged() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        insertEligibility(orderId, orderItemId, productId);
        ReviewResponse created = reviewCommandService.createReview(
                new CreateReviewRequest(productId, orderId, orderItemId, 5, "Great", "Nice", List.of()),
                CUSTOMER_ID);
        reset(reviewEventPublisher);
        UpdateReviewRequest request = new UpdateReviewRequest(5, "Only title", null, null);

        // when
        reviewCommandService.updateReview(created.id(), request, CUSTOMER_ID);

        // then
        verify(reviewEventPublisher, never()).publishProductRatingUpdated(
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("updateReview lanza ReviewOwnershipException cuando otro customer intenta editarla")
    void updateReview_shouldThrowWhenEditedByNonOwner() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        insertEligibility(orderId, orderItemId, productId);
        ReviewResponse created = reviewCommandService.createReview(
                new CreateReviewRequest(productId, orderId, orderItemId, 5, "Great", "Nice", List.of()),
                CUSTOMER_ID);

        // when
        // then
        assertThatThrownBy(() -> reviewCommandService.updateReview(
                created.id(), new UpdateReviewRequest(4, "Hijacked", "No", null), ANOTHER_CUSTOMER_ID))
                .isInstanceOf(com.ekko.review_service.exception.ReviewOwnershipException.class);
    }

    // ---------- deleteReview ----------

    @Test
    @DisplayName("deleteReview borra la review y sus imágenes y votos por cascade en la BD")
    void deleteReview_shouldCascadeDeleteImagesAndVotes() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        insertEligibility(orderId, orderItemId, productId);
        ReviewResponse created = reviewCommandService.createReview(
                new CreateReviewRequest(productId, orderId, orderItemId, 5, "Great", "Nice", List.of("img")),
                CUSTOMER_ID);
        jdbcTemplate.update(
                "INSERT INTO review_helpful_votes (review_id, customer_id) VALUES (?, ?)",
                created.id(), ANOTHER_CUSTOMER_ID);

        // when
        reviewCommandService.deleteReview(created.id(), CUSTOMER_ID);

        // then
        assertThat(reviewRepository.existsById(created.id())).isFalse();
        Long images = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM review_images WHERE review_id = ?", Long.class, created.id());
        Long votes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM review_helpful_votes WHERE review_id = ?", Long.class, created.id());
        assertThat(images).isZero();
        assertThat(votes).isZero();
    }

    @Test
    @DisplayName("deleteReview publica el recálculo final del producto (0 reviews después del borrado)")
    void deleteReview_shouldPublishFinalRecalculationAfterDelete() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        insertEligibility(orderId, orderItemId, productId);
        ReviewResponse created = reviewCommandService.createReview(
                new CreateReviewRequest(productId, orderId, orderItemId, 5, "Great", "Nice", List.of()),
                CUSTOMER_ID);
        reset(reviewEventPublisher);

        // when
        reviewCommandService.deleteReview(created.id(), CUSTOMER_ID);

        // then
        ArgumentCaptor<ProductRatingUpdatedEvent> captor = ArgumentCaptor.forClass(ProductRatingUpdatedEvent.class);
        verify(reviewEventPublisher).publishProductRatingUpdated(captor.capture());
        assertThat(captor.getValue().productId()).isEqualTo(productId);
        assertThat(captor.getValue().averageRating()).isEqualByComparingTo(new BigDecimal("0.0"));
        assertThat(captor.getValue().reviewCount()).isZero();
    }

    // ---------- admin operations ----------

    @Test
    @DisplayName("adminUpdateStatus oculta la review y el recálculo deja de contarla")
    void adminUpdateStatus_shouldHideReviewAndExcludeFromAverage() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        insertEligibility(orderId, orderItemId, productId);
        ReviewResponse created = reviewCommandService.createReview(
                new CreateReviewRequest(productId, orderId, orderItemId, 5, "Great", "Nice", List.of()),
                CUSTOMER_ID);
        reset(reviewEventPublisher);

        // when
        ReviewResponse updated = reviewCommandService.adminUpdateStatus(created.id(), ReviewStatus.HIDDEN, ADMIN_ID);

        // then
        assertThat(updated.status()).isEqualTo(ReviewStatus.HIDDEN);
        String reviewedBy = jdbcTemplate.queryForObject(
                "SELECT reviewed_by FROM reviews WHERE id = ?", String.class, created.id());
        assertThat(reviewedBy).isEqualTo(ADMIN_ID);

        ArgumentCaptor<ProductRatingUpdatedEvent> captor = ArgumentCaptor.forClass(ProductRatingUpdatedEvent.class);
        verify(reviewEventPublisher).publishProductRatingUpdated(captor.capture());
        assertThat(captor.getValue().averageRating()).isEqualByComparingTo(new BigDecimal("0.0"));
        assertThat(captor.getValue().reviewCount()).isZero();
    }

    @Test
    @DisplayName("adminUpdateContent corrige el contenido y re-dispara el recálculo si cambia el rating")
    void adminUpdateContent_shouldFixContentAndRecalculateWhenRatingChanges() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        insertEligibility(orderId, orderItemId, productId);
        ReviewResponse created = reviewCommandService.createReview(
                new CreateReviewRequest(productId, orderId, orderItemId, 5, "Great", "Nice", List.of()),
                CUSTOMER_ID);
        reset(reviewEventPublisher);

        // when
        ReviewResponse updated = reviewCommandService.adminUpdateContent(
                created.id(), new AdminUpdateContentRequest(1, "Fixed", "Corrected"), ADMIN_ID);

        // then
        assertThat(updated.rating()).isEqualTo(1);
        assertThat(updated.title()).isEqualTo("Fixed");
        assertThat(updated.comment()).isEqualTo("Corrected");

        ArgumentCaptor<ProductRatingUpdatedEvent> captor = ArgumentCaptor.forClass(ProductRatingUpdatedEvent.class);
        verify(reviewEventPublisher).publishProductRatingUpdated(captor.capture());
        assertThat(captor.getValue().averageRating()).isEqualByComparingTo(new BigDecimal("1.0"));
    }

    @Test
    @DisplayName("adminDeleteReview borra una review de otro customer y limpia sus dependencias")
    void adminDeleteReview_shouldDeleteAnyReviewAndCleanupDependencies() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        insertEligibility(orderId, orderItemId, productId);
        ReviewResponse created = reviewCommandService.createReview(
                new CreateReviewRequest(productId, orderId, orderItemId, 5, "Great", "Nice", List.of("img")),
                CUSTOMER_ID);
        jdbcTemplate.update(
                "INSERT INTO review_helpful_votes (review_id, customer_id) VALUES (?, ?)",
                created.id(), ANOTHER_CUSTOMER_ID);

        // when
        reviewCommandService.adminDeleteReview(created.id());

        // then
        assertThat(reviewRepository.existsById(created.id())).isFalse();
        Long images = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM review_images WHERE review_id = ?", Long.class, created.id());
        Long votes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM review_helpful_votes WHERE review_id = ?", Long.class, created.id());
        assertThat(images).isZero();
        assertThat(votes).isZero();
    }

    @Test
    @DisplayName("adminDeleteReview lanza ReviewNotFoundException cuando la review no existe")
    void adminDeleteReview_shouldThrowReviewNotFoundWhenReviewDoesNotExist() {
        // when
        // then
        assertThatThrownBy(() -> reviewCommandService.adminDeleteReview(UUID.randomUUID()))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    private void insertEligibility(UUID orderId, UUID orderItemId, UUID productId) {
        jdbcTemplate.update("""
                INSERT INTO eligible_reviews (order_id, order_item_id, product_id, seller_keycloak_id, customer_id)
                VALUES (?, ?, ?, ?, ?)
                """, orderId, orderItemId, productId, SELLER_KEYCLOAK_ID, CUSTOMER_ID);
    }
}