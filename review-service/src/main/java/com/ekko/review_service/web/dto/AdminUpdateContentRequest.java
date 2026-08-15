package com.ekko.review_service.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUpdateContentRequest(
        @NotNull @Min(1) @Max(5) Integer rating,
        @Size(max = 255) String title,
        String comment
) {
}