package com.ekko.product_service.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Product status")
public enum ProductStatus {
    DRAFT,
    PENDING_REVIEW,
    ACTIVE,
    REJECTED
}