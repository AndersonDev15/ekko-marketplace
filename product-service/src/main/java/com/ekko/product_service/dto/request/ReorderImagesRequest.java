package com.ekko.product_service.dto.request;

import java.util.List;
import java.util.UUID;

public record ReorderImagesRequest(
        List<UUID> imageIds
) {
}
