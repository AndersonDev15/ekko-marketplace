package com.ekko.review_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateReviewRequest(
        @Schema(description = "Rating (1-5)", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Min(1) @Max(5) Integer rating,

        @Schema(description = "Review title", example = "Updated title", maxLength = 255)
        @Size(max = 255) String title,

        @Schema(description = "Review comment", example = "Updated comment")
        String comment,

        @Schema(description = "Image URLs", example = "[\"https://example.com/image1.jpg\"]")
        List<@NotBlank @Size(max = 500) String> imageUrls
) {
}