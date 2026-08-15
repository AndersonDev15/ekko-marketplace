package com.ekko.review_service.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateReviewRequest(
        @NotNull UUID productId,
        @NotNull UUID orderId,
        @NotNull UUID orderItemId,
        @NotNull @Min(1) @Max(5) Integer rating,
        @Size(max = 255) String title,
        String comment,
        List<@NotBlank @Size(max = 500) String> imageUrls
) {
}