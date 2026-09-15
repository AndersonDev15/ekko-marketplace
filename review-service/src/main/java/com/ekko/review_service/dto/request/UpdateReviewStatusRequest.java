package com.ekko.review_service.dto.request;

import com.ekko.review_service.enums.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateReviewStatusRequest(
        @Schema(description = "New review status", example = "HIDDEN", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull ReviewStatus newStatus
) {
}