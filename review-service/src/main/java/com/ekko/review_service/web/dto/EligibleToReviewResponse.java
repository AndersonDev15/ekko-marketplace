package com.ekko.review_service.web.dto;

import java.util.UUID;

public record EligibleToReviewResponse(
        UUID orderId,
        UUID orderItemId,
        UUID productId
) {
}