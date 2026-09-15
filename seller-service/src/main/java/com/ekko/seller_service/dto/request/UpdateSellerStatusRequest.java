package com.ekko.seller_service.dto.request;

import com.ekko.seller_service.enums.SellerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateSellerStatusRequest(
        @Schema(description = "New seller status", example = "ACTIVE", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull SellerStatus status
) {}