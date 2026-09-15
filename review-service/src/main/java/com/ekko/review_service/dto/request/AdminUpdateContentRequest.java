package com.ekko.review_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUpdateContentRequest(
        @Schema(description = "Rating (1-5)", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Min(1) @Max(5) Integer rating,

        @Schema(description = "Review title", example = "Corrected title", maxLength = 255)
        @Size(max = 255) String title,

        @Schema(description = "Review comment", example = "Corrected comment")
        String comment
) {
}