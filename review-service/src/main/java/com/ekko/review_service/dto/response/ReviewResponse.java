package com.ekko.review_service.dto.response;

import com.ekko.review_service.enums.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReviewResponse(
        @Schema(description = "Review ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Product ID", example = "550e8400-e29b-41d4-a716-446655440001")
        UUID productId,

        @Schema(description = "Order ID", example = "550e8400-e29b-41d4-a716-446655440002")
        UUID orderId,

        @Schema(description = "Order Item ID", example = "550e8400-e29b-41d4-a716-446655440003")
        UUID orderItemId,

        @Schema(description = "Rating (1-5)", example = "5")
        Integer rating,

        @Schema(description = "Review title", example = "Great product!")
        String title,

        @Schema(description = "Review comment", example = "This product exceeded my expectations.")
        String comment,

        @Schema(description = "Review status", example = "VISIBLE")
        ReviewStatus status,

        @Schema(description = "Whether the review is a verified purchase", example = "true")
        Boolean isVerifiedPurchase,

        @Schema(description = "Image URLs")
        List<String> imageUrls,

        @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00")
        LocalDateTime createdAt,

        @Schema(description = "Last update timestamp", example = "2024-01-15T10:30:00")
        LocalDateTime updatedAt,

        @Schema(description = "Number of helpful votes", example = "10")
        long helpfulCount
) {
}