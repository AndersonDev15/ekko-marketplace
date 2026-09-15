package com.ekko.review_service.service;

import com.ekko.review_service.entity.EligibleReview;
import com.ekko.review_service.entity.Review;
import com.ekko.review_service.entity.ReviewImage;
import com.ekko.review_service.enums.ReviewStatus;
import com.ekko.review_service.event.RatingRecalculationRequestedEvent;
import com.ekko.review_service.exception.NotEligibleToReviewException;
import com.ekko.review_service.exception.ReviewAlreadyExistsException;
import com.ekko.review_service.exception.ReviewEditWindowExpiredException;
import com.ekko.review_service.exception.ReviewNotFoundException;
import com.ekko.review_service.exception.ReviewOwnershipException;
import com.ekko.review_service.mapper.ReviewMapper;
import com.ekko.review_service.messaging.ReviewEventPublisher;
import com.ekko.review_service.messaging.dto.publish.ReviewCreatedEvent;
import com.ekko.review_service.repository.ReviewImageRepository;
import com.ekko.review_service.repository.ReviewRepository;
import com.ekko.review_service.dto.request.AdminUpdateContentRequest;
import com.ekko.review_service.dto.request.CreateReviewRequest;
import com.ekko.review_service.dto.response.ReviewResponse;
import com.ekko.review_service.dto.request.UpdateReviewRequest;
import com.ekko.review_service.validator.ReviewEditWindowValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ekko.review_service.support.ReviewTestDataBuilder.aReview;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewCommandServiceImplTest {

    private static final String CUSTOMER_ID = "customer-1";
    private static final String ANOTHER_CUSTOMER_ID = "customer-2";
    private static final String ADMIN_ID = "admin-1";
    private static final UUID REVIEW_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID ORDER_ITEM_ID = UUID.randomUUID();
    private static final UUID SELLER_KEYCLOAK_ID = UUID.randomUUID();

    @Mock
    private EligibilityService eligibilityService;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewImageRepository reviewImageRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private ReviewEventPublisher reviewEventPublisher;

    @Mock
    private ReviewEditWindowValidator editWindowValidator;

    @InjectMocks
    private ReviewCommandServiceImpl reviewCommandService;

    // ---------- createReview ----------

    @Test
    @DisplayName("createReview persiste review, publica evento y retorna el ReviewResponse")
    void createReview_shouldPersistReviewPublishEventAndReturnResponse() {
        // given
        CreateReviewRequest request = new CreateReviewRequest(
                PRODUCT_ID, ORDER_ID, ORDER_ITEM_ID, 5, "Great", "Nice");
        Review review = aReview()
                .withProductId(PRODUCT_ID)
                .withOrderId(ORDER_ID)
                .withOrderItemId(ORDER_ITEM_ID)
                .withCustomerId(CUSTOMER_ID)
                .build();
        Review saved = aReview()
                .withId(REVIEW_ID)
                .withProductId(PRODUCT_ID)
                .withOrderId(ORDER_ID)
                .withOrderItemId(ORDER_ITEM_ID)
                .withCustomerId(CUSTOMER_ID)
                .build();
        ReviewResponse response = response(saved);
        when(eligibilityService.assertEligible(CUSTOMER_ID, ORDER_ITEM_ID, ORDER_ID, PRODUCT_ID))
                .thenReturn(eligibleReview());
        when(reviewMapper.toEntity(request, CUSTOMER_ID)).thenReturn(review);
        when(reviewRepository.findByOrderItemIdAndCustomerId(ORDER_ITEM_ID, CUSTOMER_ID))
                .thenReturn(Optional.empty());
        when(reviewRepository.save(review)).thenReturn(saved);
        when(reviewMapper.toResponse(eq(saved), anyList())).thenReturn(response);

        // when
        ReviewResponse result = reviewCommandService.createReview(request, CUSTOMER_ID);

        // then
        assertThat(result).isEqualTo(response);
        verify(reviewRepository).save(review);
        verify(reviewImageRepository, never()).save(any(ReviewImage.class));
        ArgumentCaptor<RatingRecalculationRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(RatingRecalculationRequestedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().productId()).isEqualTo(PRODUCT_ID);
    }

    @Test
    @DisplayName("createReview publica el evento review.created con los datos de la review guardada")
    void createReview_shouldPublishReviewCreatedEventWithSavedReviewDetails() {
        // given
        CreateReviewRequest request = new CreateReviewRequest(
                PRODUCT_ID, ORDER_ID, ORDER_ITEM_ID, 5, "Great", "Nice");
        Review review = aReview()
                .withProductId(PRODUCT_ID)
                .withOrderId(ORDER_ID)
                .withOrderItemId(ORDER_ITEM_ID)
                .withCustomerId(CUSTOMER_ID)
                .build();
        Review saved = aReview()
                .withId(REVIEW_ID)
                .withProductId(PRODUCT_ID)
                .withOrderId(ORDER_ID)
                .withOrderItemId(ORDER_ITEM_ID)
                .withCustomerId(CUSTOMER_ID)
                .build();
        when(eligibilityService.assertEligible(CUSTOMER_ID, ORDER_ITEM_ID, ORDER_ID, PRODUCT_ID))
                .thenReturn(eligibleReview());
        when(reviewMapper.toEntity(request, CUSTOMER_ID)).thenReturn(review);
        when(reviewRepository.findByOrderItemIdAndCustomerId(ORDER_ITEM_ID, CUSTOMER_ID))
                .thenReturn(Optional.empty());
        when(reviewRepository.save(review)).thenReturn(saved);
        when(reviewMapper.toResponse(eq(saved), anyList())).thenReturn(response(saved));

        // when
        reviewCommandService.createReview(request, CUSTOMER_ID);

        // then
        ArgumentCaptor<ReviewCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ReviewCreatedEvent.class);
        verify(reviewEventPublisher).publishReviewCreated(eventCaptor.capture());
        ReviewCreatedEvent event = eventCaptor.getValue();
        assertThat(event.reviewId()).isEqualTo(REVIEW_ID);
        assertThat(event.productId()).isEqualTo(PRODUCT_ID);
        assertThat(event.orderId()).isEqualTo(ORDER_ID);
        assertThat(event.orderItemId()).isEqualTo(ORDER_ITEM_ID);
        assertThat(event.sellerKeycloakId()).isEqualTo(SELLER_KEYCLOAK_ID);
        assertThat(event.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(event.rating()).isEqualTo(saved.getRating());
        assertThat(event.title()).isEqualTo(saved.getTitle());
        assertThat(event.comment()).isEqualTo(saved.getComment());
        assertThat(event.createdAt()).isEqualTo(saved.getCreatedAt());
    }

    @Test
    @DisplayName("createReview no publica el evento review.created cuando el customer no es elegible")
    void createReview_shouldNotPublishCreatedEventWhenCustomerNotEligible() {
        // given
        CreateReviewRequest request = new CreateReviewRequest(
                PRODUCT_ID, ORDER_ID, ORDER_ITEM_ID, 5, "Great", "Nice");
        doThrow(new NotEligibleToReviewException())
                .when(eligibilityService).assertEligible(CUSTOMER_ID, ORDER_ITEM_ID, ORDER_ID, PRODUCT_ID);

        // when
        // then
        assertThatThrownBy(() -> reviewCommandService.createReview(request, CUSTOMER_ID))
                .isInstanceOf(NotEligibleToReviewException.class);
        verify(reviewRepository, never()).save(any(Review.class));
        verify(applicationEventPublisher, never()).publishEvent(any());
        verify(reviewEventPublisher, never()).publishReviewCreated(any());
    }

    @Test
    @DisplayName("createReview no persiste nada cuando el customer no es elegible")
    void createReview_shouldNotPersistAnythingWhenCustomerNotEligible() {
        // given
        CreateReviewRequest request = new CreateReviewRequest(
                PRODUCT_ID, ORDER_ID, ORDER_ITEM_ID, 5, "Great", "Nice");
        doThrow(new NotEligibleToReviewException())
                .when(eligibilityService).assertEligible(CUSTOMER_ID, ORDER_ITEM_ID, ORDER_ID, PRODUCT_ID);

        // when
        // then
        assertThatThrownBy(() -> reviewCommandService.createReview(request, CUSTOMER_ID))
                .isInstanceOf(NotEligibleToReviewException.class);
        verify(reviewRepository, never()).save(any(Review.class));
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("createReview lanza ReviewAlreadyExistsException cuando ya existe review para ese orderItem+customer")
    void createReview_shouldThrowReviewAlreadyExistsWhenReviewForOrderItemAndCustomerExists() {
        // given
        CreateReviewRequest request = new CreateReviewRequest(
                PRODUCT_ID, ORDER_ID, ORDER_ITEM_ID, 5, "Great", "Nice");
        when(reviewRepository.findByOrderItemIdAndCustomerId(ORDER_ITEM_ID, CUSTOMER_ID))
                .thenReturn(Optional.of(aReview().build()));

        // when
        // then
        assertThatThrownBy(() -> reviewCommandService.createReview(request, CUSTOMER_ID))
                .isInstanceOf(ReviewAlreadyExistsException.class);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("createReview no persiste imágenes (se gestionan en updateReview)")
    void createReview_shouldNotPersistImages() {
        // given
        CreateReviewRequest request = new CreateReviewRequest(
                PRODUCT_ID, ORDER_ID, ORDER_ITEM_ID, 5, "Great", "Nice");
        Review review = aReview().withOrderItemId(ORDER_ITEM_ID).build();
        Review saved = aReview().withId(REVIEW_ID).withOrderItemId(ORDER_ITEM_ID).build();
        when(eligibilityService.assertEligible(CUSTOMER_ID, ORDER_ITEM_ID, ORDER_ID, PRODUCT_ID))
                .thenReturn(eligibleReview());
        when(reviewMapper.toEntity(request, CUSTOMER_ID)).thenReturn(review);
        when(reviewRepository.findByOrderItemIdAndCustomerId(ORDER_ITEM_ID, CUSTOMER_ID))
                .thenReturn(Optional.empty());
        when(reviewRepository.save(review)).thenReturn(saved);
        when(reviewMapper.toResponse(eq(saved), anyList())).thenReturn(response(saved));

        // when
        reviewCommandService.createReview(request, CUSTOMER_ID);

        // then
        verify(reviewImageRepository, never()).save(any(ReviewImage.class));
    }

    // ---------- updateReview ----------

    @Test
    @DisplayName("updateReview actualiza los 4 campos editables cuando es dueño y está dentro de la ventana")
    void updateReview_shouldUpdateEditableFieldsWhenOwnerAndWithinWindow() {
        // given
        Review review = aReview()
                .withId(REVIEW_ID)
                .withProductId(PRODUCT_ID)
                .withCustomerId(CUSTOMER_ID)
                .withRating(5)
                .withTitle("Old title")
                .withComment("Old comment")
                .build();
        UpdateReviewRequest request = new UpdateReviewRequest(4, "New title", "New comment", null);
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewImageRepository.findByReviewIdOrderBySortOrderAsc(REVIEW_ID)).thenReturn(List.of());
        when(reviewMapper.toResponse(review, List.of())).thenReturn(response(review));

        // when
        reviewCommandService.updateReview(REVIEW_ID, request, CUSTOMER_ID);

        // then
        assertThat(review.getRating()).isEqualTo(4);
        assertThat(review.getTitle()).isEqualTo("New title");
        assertThat(review.getComment()).isEqualTo("New comment");
        ArgumentCaptor<RatingRecalculationRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(RatingRecalculationRequestedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().productId()).isEqualTo(PRODUCT_ID);
    }

    @Test
    @DisplayName("updateReview lanza ReviewNotFoundException cuando la review no existe")
    void updateReview_shouldThrowReviewNotFoundWhenReviewDoesNotExist() {
        // given
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> reviewCommandService.updateReview(
                REVIEW_ID, new UpdateReviewRequest(4, "New", "New", null), CUSTOMER_ID))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    @DisplayName("updateReview lanza ReviewOwnershipException cuando el customer no es el dueño")
    void updateReview_shouldThrowReviewOwnershipWhenCustomerIsNotOwner() {
        // given
        Review review = aReview().withId(REVIEW_ID).withCustomerId(ANOTHER_CUSTOMER_ID).build();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        // when
        // then
        assertThatThrownBy(() -> reviewCommandService.updateReview(
                REVIEW_ID, new UpdateReviewRequest(4, "New", "New", null), CUSTOMER_ID))
                .isInstanceOf(ReviewOwnershipException.class);
    }

    @Test
    @DisplayName("updateReview lanza ReviewEditWindowExpiredException cuando pasaron más de 15 días desde created_at")
    void updateReview_shouldThrowEditWindowExpiredWhenReviewIsOlderThan15Days() {
        // given
        Review review = aReview()
                .withId(REVIEW_ID)
                .withCustomerId(CUSTOMER_ID)
                .withCreatedAt(LocalDateTime.now().minusDays(16))
                .build();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        doThrow(new ReviewEditWindowExpiredException())
                .when(editWindowValidator).assertWithinWindow(any());

        // when
        // then
        assertThatThrownBy(() -> reviewCommandService.updateReview(
                REVIEW_ID, new UpdateReviewRequest(4, "New", "New", null), CUSTOMER_ID))
                .isInstanceOf(ReviewEditWindowExpiredException.class);
    }

    @Test
    @DisplayName("updateReview permite editar exactamente en el borde de los 15 días")
    void updateReview_shouldAllowEditingExactlyAtThe15DayBoundary() {
        // given
        Review review = aReview()
                .withId(REVIEW_ID)
                .withCustomerId(CUSTOMER_ID)
                .withRating(5)
                .withCreatedAt(LocalDateTime.now().minusDays(15))
                .build();
        UpdateReviewRequest request = new UpdateReviewRequest(4, "New title", "New comment", null);
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewImageRepository.findByReviewIdOrderBySortOrderAsc(REVIEW_ID)).thenReturn(List.of());
        when(reviewMapper.toResponse(review, List.of())).thenReturn(response(review));
        doNothing().when(editWindowValidator).assertWithinWindow(any());

        // when
        // then
        assertThatCode(() -> reviewCommandService.updateReview(REVIEW_ID, request, CUSTOMER_ID))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("updateReview permite editar con 15 días y algunas horas extra, porque toDays() trunca a 15")
    void updateReview_shouldAllowEditingWith15DaysAndAHalfSinceToDaysTruncates() {
        // given
        Review review = aReview()
                .withId(REVIEW_ID)
                .withCustomerId(CUSTOMER_ID)
                .withRating(5)
                .withCreatedAt(LocalDateTime.now().minusDays(15).minusHours(1))
                .build();
        UpdateReviewRequest request = new UpdateReviewRequest(4, "New title", "New comment", null);
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewImageRepository.findByReviewIdOrderBySortOrderAsc(REVIEW_ID)).thenReturn(List.of());
        when(reviewMapper.toResponse(review, List.of())).thenReturn(response(review));
        doNothing().when(editWindowValidator).assertWithinWindow(any());

        // when
        // then
        assertThatCode(() -> reviewCommandService.updateReview(REVIEW_ID, request, CUSTOMER_ID))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("updateReview reemplaza las imágenes cuando el request trae una lista")
    void updateReview_shouldReplaceImagesWhenRequestContainsImageList() {
        // given
        Review review = aReview()
                .withId(REVIEW_ID)
                .withCustomerId(CUSTOMER_ID)
                .withRating(5)
                .build();
        UpdateReviewRequest request = new UpdateReviewRequest(5, "New title", "New comment", List.of("new-url"));
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewImageRepository.findByReviewIdOrderBySortOrderAsc(REVIEW_ID)).thenReturn(List.of());
        when(reviewMapper.toResponse(review, List.of())).thenReturn(response(review));
        doNothing().when(editWindowValidator).assertWithinWindow(any());

        // when
        reviewCommandService.updateReview(REVIEW_ID, request, CUSTOMER_ID);

        // then
        verify(reviewImageRepository).deleteByReviewId(REVIEW_ID);
        verify(reviewImageRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("updateReview no toca las imágenes cuando el request no trae lista")
    void updateReview_shouldNotTouchImagesWhenRequestHasNoImageList() {
        // given
        Review review = aReview().withId(REVIEW_ID).withCustomerId(CUSTOMER_ID).withRating(5).build();
        UpdateReviewRequest request = new UpdateReviewRequest(5, "New title", "New comment", null);
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewImageRepository.findByReviewIdOrderBySortOrderAsc(REVIEW_ID)).thenReturn(List.of());
        when(reviewMapper.toResponse(review, List.of())).thenReturn(response(review));
        doNothing().when(editWindowValidator).assertWithinWindow(any());

        // when
        reviewCommandService.updateReview(REVIEW_ID, request, CUSTOMER_ID);

        // then
        verify(reviewImageRepository, never()).deleteByReviewId(REVIEW_ID);
        verify(reviewImageRepository, never()).save(any(ReviewImage.class));
    }

    @Test
    @DisplayName("updateReview publica evento de recálculo solo cuando el rating cambió")
    void updateReview_shouldPublishRecalculationEventWhenRatingChanged() {
        // given
        Review review = aReview().withId(REVIEW_ID).withCustomerId(CUSTOMER_ID).withRating(5).build();
        UpdateReviewRequest request = new UpdateReviewRequest(4, "New title", "New comment", null);
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewImageRepository.findByReviewIdOrderBySortOrderAsc(REVIEW_ID)).thenReturn(List.of());
        when(reviewMapper.toResponse(review, List.of())).thenReturn(response(review));
        doNothing().when(editWindowValidator).assertWithinWindow(any());

        // when
        reviewCommandService.updateReview(REVIEW_ID, request, CUSTOMER_ID);

        // then
        verify(applicationEventPublisher, times(1)).publishEvent(any(RatingRecalculationRequestedEvent.class));
    }

    @Test
    @DisplayName("updateReview no publica el evento cuando el rating no cambió")
    void updateReview_shouldNotPublishEventWhenRatingDidNotChange() {
        // given
        Review review = aReview().withId(REVIEW_ID).withCustomerId(CUSTOMER_ID).withRating(5).build();
        UpdateReviewRequest request = new UpdateReviewRequest(5, "New title", "New comment", null);
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewImageRepository.findByReviewIdOrderBySortOrderAsc(REVIEW_ID)).thenReturn(List.of());
        when(reviewMapper.toResponse(review, List.of())).thenReturn(response(review));
        doNothing().when(editWindowValidator).assertWithinWindow(any());

        // when
        reviewCommandService.updateReview(REVIEW_ID, request, CUSTOMER_ID);

        // then
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    // ---------- deleteReview ----------

    @Test
    @DisplayName("deleteReview elimina la review y publica el evento con el productId capturado antes del delete")
    void deleteReview_shouldDeleteReviewAndPublishEventWithProductId() {
        // given
        Review review = aReview()
                .withId(REVIEW_ID)
                .withProductId(PRODUCT_ID)
                .withCustomerId(CUSTOMER_ID)
                .build();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        // when
        reviewCommandService.deleteReview(REVIEW_ID, CUSTOMER_ID);

        // then
        verify(reviewRepository).delete(review);
        ArgumentCaptor<RatingRecalculationRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(RatingRecalculationRequestedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().productId()).isEqualTo(PRODUCT_ID);
    }

    @Test
    @DisplayName("deleteReview lanza ReviewNotFoundException cuando la review no existe")
    void deleteReview_shouldThrowReviewNotFoundWhenReviewDoesNotExist() {
        // given
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> reviewCommandService.deleteReview(REVIEW_ID, CUSTOMER_ID))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    @DisplayName("deleteReview lanza ReviewOwnershipException cuando el customer no es el dueño")
    void deleteReview_shouldThrowReviewOwnershipWhenCustomerIsNotOwner() {
        // given
        Review review = aReview().withId(REVIEW_ID).withCustomerId(ANOTHER_CUSTOMER_ID).build();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        // when
        // then
        assertThatThrownBy(() -> reviewCommandService.deleteReview(REVIEW_ID, CUSTOMER_ID))
                .isInstanceOf(ReviewOwnershipException.class);
    }

    @Test
    @DisplayName("deleteReview no valida ventana de tiempo: un review antiguo también se puede borrar")
    void deleteReview_shouldNotValidateEditWindowForOldReview() {
        // given
        Review review = aReview()
                .withId(REVIEW_ID)
                .withProductId(PRODUCT_ID)
                .withCustomerId(CUSTOMER_ID)
                .withCreatedAt(LocalDateTime.now().minusDays(100))
                .build();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        // when
        // then
        assertThatCode(() -> reviewCommandService.deleteReview(REVIEW_ID, CUSTOMER_ID))
                .doesNotThrowAnyException();
        verify(reviewRepository).delete(review);
    }

    // ---------- adminUpdateStatus ----------

    @Test
    @DisplayName("adminUpdateStatus actualiza status, reviewedBy y reviewedAt")
    void adminUpdateStatus_shouldUpdateStatusReviewedByAndReviewedAt() {
        // given
        Review review = aReview().withId(REVIEW_ID).withProductId(PRODUCT_ID).build();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewImageRepository.findByReviewIdOrderBySortOrderAsc(REVIEW_ID)).thenReturn(List.of());
        when(reviewMapper.toResponse(review, List.of())).thenReturn(response(review));

        // when
        reviewCommandService.adminUpdateStatus(REVIEW_ID, ReviewStatus.HIDDEN, ADMIN_ID);

        // then
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.HIDDEN);
        assertThat(review.getReviewedBy()).isEqualTo(ADMIN_ID);
        assertThat(review.getReviewedAt()).isNotNull();
        verify(applicationEventPublisher, times(1)).publishEvent(any(RatingRecalculationRequestedEvent.class));
    }

    @Test
    @DisplayName("adminUpdateStatus publica el evento siempre, incluso si el status no cambia de valor")
    void adminUpdateStatus_shouldAlwaysPublishEventEvenWhenStatusIsUnchanged() {
        // given
        Review review = aReview().withId(REVIEW_ID).withProductId(PRODUCT_ID).hidden().build();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewImageRepository.findByReviewIdOrderBySortOrderAsc(REVIEW_ID)).thenReturn(List.of());
        when(reviewMapper.toResponse(review, List.of())).thenReturn(response(review));

        // when
        reviewCommandService.adminUpdateStatus(REVIEW_ID, ReviewStatus.HIDDEN, ADMIN_ID);

        // then
        verify(applicationEventPublisher, times(1)).publishEvent(any(RatingRecalculationRequestedEvent.class));
    }

    @Test
    @DisplayName("adminUpdateStatus lanza ReviewNotFoundException cuando la review no existe")
    void adminUpdateStatus_shouldThrowReviewNotFoundWhenReviewDoesNotExist() {
        // given
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> reviewCommandService.adminUpdateStatus(REVIEW_ID, ReviewStatus.HIDDEN, ADMIN_ID))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    @DisplayName("adminUpdateStatus no restringe transiciones: VISIBLE->HIDDEN y HIDDEN->VISIBLE funcionan")
    void adminUpdateStatus_shouldAllowBothStatusTransitions() {
        // given
        Review visible = aReview().withId(REVIEW_ID).build();
        Review hidden = aReview().withId(UUID.randomUUID()).hidden().build();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(visible));
        when(reviewRepository.findById(hidden.getId())).thenReturn(Optional.of(hidden));
        when(reviewImageRepository.findByReviewIdOrderBySortOrderAsc(any())).thenReturn(List.of());
        when(reviewMapper.toResponse(any(Review.class), anyList())).thenReturn(response(visible));

        // when
        reviewCommandService.adminUpdateStatus(REVIEW_ID, ReviewStatus.HIDDEN, ADMIN_ID);
        reviewCommandService.adminUpdateStatus(hidden.getId(), ReviewStatus.VISIBLE, ADMIN_ID);

        // then
        assertThat(visible.getStatus()).isEqualTo(ReviewStatus.HIDDEN);
        assertThat(hidden.getStatus()).isEqualTo(ReviewStatus.VISIBLE);
    }

    // ---------- adminUpdateContent ----------

    @Test
    @DisplayName("adminUpdateContent actualiza title, comment, rating, reviewedBy y reviewedAt")
    void adminUpdateContent_shouldUpdateAllEditableFieldsAndReviewerMetadata() {
        // given
        Review review = aReview()
                .withId(REVIEW_ID)
                .withProductId(PRODUCT_ID)
                .withRating(5)
                .withTitle("Old")
                .withComment("Old")
                .build();
        AdminUpdateContentRequest request = new AdminUpdateContentRequest(3, "New title", "New comment");
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewImageRepository.findByReviewIdOrderBySortOrderAsc(REVIEW_ID)).thenReturn(List.of());
        when(reviewMapper.toResponse(review, List.of())).thenReturn(response(review));

        // when
        reviewCommandService.adminUpdateContent(REVIEW_ID, request, ADMIN_ID);

        // then
        assertThat(review.getRating()).isEqualTo(3);
        assertThat(review.getTitle()).isEqualTo("New title");
        assertThat(review.getComment()).isEqualTo("New comment");
        assertThat(review.getReviewedBy()).isEqualTo(ADMIN_ID);
        assertThat(review.getReviewedAt()).isNotNull();
        verify(applicationEventPublisher, times(1)).publishEvent(any(RatingRecalculationRequestedEvent.class));
    }

    @Test
    @DisplayName("adminUpdateContent publica el evento solo cuando el rating cambió")
    void adminUpdateContent_shouldPublishEventWhenRatingChanged() {
        // given
        Review review = aReview().withId(REVIEW_ID).withRating(5).build();
        AdminUpdateContentRequest request = new AdminUpdateContentRequest(2, "New title", "New comment");
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewImageRepository.findByReviewIdOrderBySortOrderAsc(REVIEW_ID)).thenReturn(List.of());
        when(reviewMapper.toResponse(review, List.of())).thenReturn(response(review));

        // when
        reviewCommandService.adminUpdateContent(REVIEW_ID, request, ADMIN_ID);

        // then
        verify(applicationEventPublisher, times(1)).publishEvent(any(RatingRecalculationRequestedEvent.class));
    }

    @Test
    @DisplayName("adminUpdateContent no publica el evento cuando el rating no cambió")
    void adminUpdateContent_shouldNotPublishEventWhenRatingDidNotChange() {
        // given
        Review review = aReview().withId(REVIEW_ID).withRating(5).build();
        AdminUpdateContentRequest request = new AdminUpdateContentRequest(5, "New title", "New comment");
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewImageRepository.findByReviewIdOrderBySortOrderAsc(REVIEW_ID)).thenReturn(List.of());
        when(reviewMapper.toResponse(review, List.of())).thenReturn(response(review));

        // when
        reviewCommandService.adminUpdateContent(REVIEW_ID, request, ADMIN_ID);

        // then
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("adminUpdateContent lanza ReviewNotFoundException cuando la review no existe")
    void adminUpdateContent_shouldThrowReviewNotFoundWhenReviewDoesNotExist() {
        // given
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> reviewCommandService.adminUpdateContent(
                REVIEW_ID, new AdminUpdateContentRequest(5, "New", "New"), ADMIN_ID))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    @DisplayName("adminUpdateContent no valida la ventana de edición a diferencia del updateReview de cliente")
    void adminUpdateContent_shouldNotValidateEditWindow() {
        // given
        Review review = aReview()
                .withId(REVIEW_ID)
                .withProductId(PRODUCT_ID)
                .withRating(5)
                .withCreatedAt(LocalDateTime.now().minusDays(100))
                .build();
        AdminUpdateContentRequest request = new AdminUpdateContentRequest(4, "New title", "New comment");
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewImageRepository.findByReviewIdOrderBySortOrderAsc(REVIEW_ID)).thenReturn(List.of());
        when(reviewMapper.toResponse(review, List.of())).thenReturn(response(review));

        // when
        // then
        assertThatCode(() -> reviewCommandService.adminUpdateContent(REVIEW_ID, request, ADMIN_ID))
                .doesNotThrowAnyException();
    }

    // ---------- adminDeleteReview ----------

    @Test
    @DisplayName("adminDeleteReview elimina y publica el evento con el productId capturado antes del delete")
    void adminDeleteReview_shouldDeleteReviewAndPublishEventWithProductId() {
        // given
        Review review = aReview().withId(REVIEW_ID).withProductId(PRODUCT_ID).build();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        // when
        reviewCommandService.adminDeleteReview(REVIEW_ID);

        // then
        verify(reviewRepository).delete(review);
        ArgumentCaptor<RatingRecalculationRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(RatingRecalculationRequestedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().productId()).isEqualTo(PRODUCT_ID);
    }

    @Test
    @DisplayName("adminDeleteReview lanza ReviewNotFoundException cuando la review no existe")
    void adminDeleteReview_shouldThrowReviewNotFoundWhenReviewDoesNotExist() {
        // given
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> reviewCommandService.adminDeleteReview(REVIEW_ID))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    @DisplayName("adminDeleteReview no verifica ownership a diferencia del deleteReview de cliente")
    void adminDeleteReview_shouldNotVerifyOwnership() {
        // given
        Review review = aReview()
                .withId(REVIEW_ID)
                .withProductId(PRODUCT_ID)
                .withCustomerId(ANOTHER_CUSTOMER_ID)
                .build();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        // when
        // then
        assertThatCode(() -> reviewCommandService.adminDeleteReview(REVIEW_ID))
                .doesNotThrowAnyException();
        verify(reviewRepository).delete(review);
    }

    private ReviewResponse response(Review review) {
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
                0L);
    }

    private EligibleReview eligibleReview() {
        return EligibleReview.builder()
                .orderId(ORDER_ID)
                .orderItemId(ORDER_ITEM_ID)
                .productId(PRODUCT_ID)
                .sellerKeycloakId(SELLER_KEYCLOAK_ID)
                .customerId(CUSTOMER_ID)
                .build();
    }
}