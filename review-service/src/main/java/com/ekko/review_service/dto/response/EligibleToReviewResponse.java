package com.ekko.review_service.dto.response;

import java.util.UUID;

public record EligibleToReviewResponse(
        UUID orderId,
        UUID orderItemId,
        UUID productId
) {
}