package com.ekko.review_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record EligibleToReviewResponse(
        @Schema(description = "Order ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID orderId,

        @Schema(description = "Order Item ID", example = "550e8400-e29b-41d4-a716-446655440001")
        UUID orderItemId,

        @Schema(description = "Product ID", example = "550e8400-e29b-41d4-a716-446655440002")
        UUID productId
) {
}