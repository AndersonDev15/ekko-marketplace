package com.ekko.product_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Request to reorder product images")
public record ReorderImagesRequest(
        @Schema(description = "List of image IDs in the new order", requiredMode = Schema.RequiredMode.REQUIRED)
        List<UUID> imageIds
) {
}
