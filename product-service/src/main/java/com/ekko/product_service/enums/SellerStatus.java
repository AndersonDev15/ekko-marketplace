package com.ekko.product_service.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Seller status")
public enum SellerStatus {
    PENDING_REVIEW,
    ACTIVE,
    SUSPENDED;
}