package com.ekko.review_service.support;

import com.ekko.review_service.entity.Review;
import com.ekko.review_service.enums.ReviewStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public final class ReviewTestDataBuilder {

    private UUID id;
    private UUID productId = UUID.randomUUID();
    private String customerId = "customer-1";
    private UUID orderId = UUID.randomUUID();
    private UUID orderItemId = UUID.randomUUID();
    private Integer rating = 5;
    private String title = "Great product";
    private String comment = "Works as expected";
    private ReviewStatus status = ReviewStatus.VISIBLE;
    private Boolean isVerifiedPurchase = true;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
    private LocalDateTime updatedAt = LocalDateTime.now().minusDays(1);

    private ReviewTestDataBuilder() {
    }

    public static ReviewTestDataBuilder aReview() {
        return new ReviewTestDataBuilder();
    }

    public ReviewTestDataBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public ReviewTestDataBuilder withProductId(UUID productId) {
        this.productId = productId;
        return this;
    }

    public ReviewTestDataBuilder withCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public ReviewTestDataBuilder withOrderId(UUID orderId) {
        this.orderId = orderId;
        return this;
    }

    public ReviewTestDataBuilder withOrderItemId(UUID orderItemId) {
        this.orderItemId = orderItemId;
        return this;
    }

    public ReviewTestDataBuilder withRating(Integer rating) {
        this.rating = rating;
        return this;
    }

    public ReviewTestDataBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    public ReviewTestDataBuilder withComment(String comment) {
        this.comment = comment;
        return this;
    }

    public ReviewTestDataBuilder withStatus(ReviewStatus status) {
        this.status = status;
        return this;
    }

    public ReviewTestDataBuilder hidden() {
        this.status = ReviewStatus.HIDDEN;
        return this;
    }

    public ReviewTestDataBuilder withReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
        return this;
    }

    public ReviewTestDataBuilder withReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
        return this;
    }

    public ReviewTestDataBuilder withCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Review build() {
        return Review.builder()
                .id(id)
                .productId(productId)
                .customerId(customerId)
                .orderId(orderId)
                .orderItemId(orderItemId)
                .rating(rating)
                .title(title)
                .comment(comment)
                .status(status)
                .isVerifiedPurchase(isVerifiedPurchase)
                .reviewedBy(reviewedBy)
                .reviewedAt(reviewedAt)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}