package com.ekko.review_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record ReviewImageResponse(
        @Schema(description = "Image ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Image URL", example = "https://cloudinary.com/image.jpg")
        String url,

        @Schema(description = "Sort order", example = "0")
        Integer sortOrder
) {
}
