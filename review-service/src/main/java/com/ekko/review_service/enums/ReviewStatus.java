package com.ekko.review_service.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Review status", allowableValues = {"VISIBLE", "HIDDEN"})
public enum ReviewStatus {
    VISIBLE,
    HIDDEN
}