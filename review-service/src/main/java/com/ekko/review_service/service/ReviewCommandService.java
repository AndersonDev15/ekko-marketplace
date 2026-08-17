package com.ekko.review_service.service;

import com.ekko.review_service.enums.ReviewStatus;
import com.ekko.review_service.dto.request.AdminUpdateContentRequest;
import com.ekko.review_service.dto.request.CreateReviewRequest;
import com.ekko.review_service.dto.response.ReviewResponse;
import com.ekko.review_service.dto.request.UpdateReviewRequest;

import java.util.UUID;

public interface ReviewCommandService {

    ReviewResponse createReview(CreateReviewRequest request, String customerId);

    ReviewResponse updateReview(UUID reviewId, UpdateReviewRequest request, String customerId);

    void deleteReview(UUID reviewId, String customerId);

    ReviewResponse adminUpdateStatus(UUID reviewId, ReviewStatus newStatus, String adminId);

    ReviewResponse adminUpdateContent(UUID reviewId, AdminUpdateContentRequest request, String adminId);

    void adminDeleteReview(UUID reviewId);
}