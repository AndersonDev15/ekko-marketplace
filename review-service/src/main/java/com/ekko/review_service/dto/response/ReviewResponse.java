package com.ekko.review_service.dto.response;

import com.ekko.review_service.enums.ReviewStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID productId,
        UUID orderId,
        UUID orderItemId,
        Integer rating,
        String title,
        String comment,
        ReviewStatus status,
        Boolean isVerifiedPurchase,
        List<String> imageUrls,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long helpfulCount
) {
}