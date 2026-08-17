package com.ekko.review_service.service;

import com.ekko.review_service.enums.ReviewStatus;
import com.ekko.review_service.dto.response.EligibleToReviewResponse;
import com.ekko.review_service.dto.response.ProductReviewsResponse;
import com.ekko.review_service.dto.response.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ReviewQueryService {

    ProductReviewsResponse getProductReviews(UUID productId, Pageable pageable);

    ReviewResponse getReviewById(UUID reviewId);

    Page<ReviewResponse> getMyReviews(String customerId, Pageable pageable);

    List<EligibleToReviewResponse> getEligibleToReview(String customerId);

    Page<ReviewResponse> adminGetAllReviews(ReviewStatus status, UUID productId, String customerId, Pageable pageable);
}