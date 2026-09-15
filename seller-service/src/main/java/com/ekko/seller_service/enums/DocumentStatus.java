package com.ekko.seller_service.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Document status", allowableValues = {"PENDING", "APPROVED", "REJECTED"})
public enum DocumentStatus {
    PENDING,
    APPROVED,
    REJECTED
}