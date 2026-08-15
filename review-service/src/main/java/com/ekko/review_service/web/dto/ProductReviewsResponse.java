package com.ekko.review_service.web.dto;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.Map;

public record ProductReviewsResponse(
        Page<ReviewResponse> reviews,
        BigDecimal averageRating,
        long totalReviews,
        Map<Integer, Long> ratingDistribution
) {
}