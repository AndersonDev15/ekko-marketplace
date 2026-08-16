package com.ekko.review_service.web.dto;

import com.ekko.review_service.enums.ReviewStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateReviewStatusRequest(
        @NotNull ReviewStatus newStatus
) {
}