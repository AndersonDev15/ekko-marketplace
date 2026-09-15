package com.ekko.review_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateReviewRequest(
        @Schema(description = "Product ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull UUID productId,

        @Schema(description = "Order ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull UUID orderId,

        @Schema(description = "Order Item ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull UUID orderItemId,

        @Schema(description = "Rating (1-5)", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Min(1) @Max(5) Integer rating,

        @Schema(description = "Review title", example = "Great product!", maxLength = 255)
        @Size(max = 255) String title,

        @Schema(description = "Review comment", example = "This product exceeded my expectations.")
        String comment
) {
}