package com.ekko.review_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.Map;

public record ProductReviewsResponse(
        @Schema(description = "Paginated reviews")
        Page<ReviewResponse> reviews,

        @Schema(description = "Average rating", example = "4.5")
        BigDecimal averageRating,

        @Schema(description = "Total number of reviews", example = "100")
        long totalReviews,

        @Schema(description = "Rating distribution (rating -> count)", example = "{1: 5, 2: 3, 3: 10, 4: 25, 5: 57}")
        Map<Integer, Long> ratingDistribution
) {
}