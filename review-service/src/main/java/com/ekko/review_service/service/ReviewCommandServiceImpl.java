package com.ekko.review_service.service;

import com.ekko.review_service.entity.EligibleReview;
import com.ekko.review_service.entity.Review;
import com.ekko.review_service.entity.ReviewImage;
import com.ekko.review_service.enums.ReviewStatus;
import com.ekko.review_service.event.RatingRecalculationRequestedEvent;
import com.ekko.review_service.exception.ReviewAlreadyExistsException;
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
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewCommandServiceImpl implements ReviewCommandService {

    private static final int EDIT_WINDOW_DAYS = 15;

    private final EligibilityService eligibilityService;
    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ReviewMapper reviewMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ReviewEditWindowValidator editWindowValidator;
    private final ReviewEventPublisher reviewEventPublisher;

    @Override
    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request, String customerId) {
        EligibleReview eligible = eligibilityService.assertEligible(
                customerId, request.orderItemId(), request.orderId(), request.productId());

        reviewRepository.findByOrderItemIdAndCustomerId(request.orderItemId(), customerId)
                .ifPresent(review -> {
                    throw new ReviewAlreadyExistsException();
                });

        Review saved = reviewRepository.save(reviewMapper.toEntity(request, customerId));

        applicationEventPublisher.publishEvent(new RatingRecalculationRequestedEvent(saved.getProductId()));
        reviewEventPublisher.publishReviewCreated(toCreatedEvent(saved, eligible.getSellerKeycloakId()));

        return reviewMapper.toResponse(saved, List.of());
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(UUID reviewId, UpdateReviewRequest request, String customerId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(ReviewNotFoundException::new);

        assertOwnership(review, customerId);
        editWindowValidator.assertWithinWindow(review.getCreatedAt());



        Integer previousRating = review.getRating();
        review.setRating(request.rating());
        review.setTitle(request.title());
        review.setComment(request.comment());

        if (ratingChanged(previousRating, review.getRating())) {
            applicationEventPublisher.publishEvent(new RatingRecalculationRequestedEvent(review.getProductId()));
        }

        return toResponse(review);
    }
    @Override
    @Transactional
    public void deleteReview(UUID reviewId, String customerId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(ReviewNotFoundException::new);

        assertOwnership(review, customerId);

        // review_helpful_votes has ON DELETE CASCADE (V2 migration): vote cleanup is automatic at DB level.
        UUID productId = review.getProductId();
        reviewRepository.delete(review);

        applicationEventPublisher.publishEvent(new RatingRecalculationRequestedEvent(productId));
    }

    @Override
    @Transactional
    public ReviewResponse adminUpdateStatus(UUID reviewId, ReviewStatus newStatus, String adminId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(ReviewNotFoundException::new);

        review.setStatus(newStatus);
        review.setReviewedBy(adminId);
        review.setReviewedAt(LocalDateTime.now());

        // Status change always affects whether the review counts towards the average.
        applicationEventPublisher.publishEvent(new RatingRecalculationRequestedEvent(review.getProductId()));

        return toResponse(review);
    }

    @Override
    @Transactional
    public ReviewResponse adminUpdateContent(UUID reviewId, AdminUpdateContentRequest request, String adminId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(ReviewNotFoundException::new);

        Integer previousRating = review.getRating();
        review.setRating(request.rating());
        review.setTitle(request.title());
        review.setComment(request.comment());
        review.setReviewedBy(adminId);
        review.setReviewedAt(LocalDateTime.now());

        if (ratingChanged(previousRating, review.getRating())) {
            applicationEventPublisher.publishEvent(new RatingRecalculationRequestedEvent(review.getProductId()));
        }

        return toResponse(review);
    }

    @Override
    @Transactional
    public void adminDeleteReview(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(ReviewNotFoundException::new);

        // review_helpful_votes has ON DELETE CASCADE (V2 migration): vote cleanup is automatic at DB level.
        UUID productId = review.getProductId();
        reviewRepository.delete(review);

        applicationEventPublisher.publishEvent(new RatingRecalculationRequestedEvent(productId));
    }

    private void assertOwnership(Review review, String customerId) {
        if (!review.getCustomerId().equals(customerId)) {
            throw new ReviewOwnershipException();
        }
    }

    private boolean ratingChanged(Integer previousRating, Integer newRating) {
        return previousRating != null && !previousRating.equals(newRating);
    }


    private ReviewResponse toResponse(Review review) {
        List<ReviewImage> images = reviewImageRepository.findByReviewIdOrderBySortOrderAsc(review.getId());
        return reviewMapper.toResponse(review, images);
    }

    private ReviewCreatedEvent toCreatedEvent(Review review, UUID sellerKeycloakId) {
        return new ReviewCreatedEvent(
                review.getId(),
                review.getProductId(),
                review.getOrderId(),
                review.getOrderItemId(),
                sellerKeycloakId,
                review.getCustomerId(),
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                review.getCreatedAt());
    }
}