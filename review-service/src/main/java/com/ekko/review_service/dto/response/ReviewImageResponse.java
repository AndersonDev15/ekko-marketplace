package com.ekko.review_service.dto.response;

import java.util.UUID;

public record ReviewImageResponse(
        UUID id,
        String url,
        Integer sortOrder
) {
}
