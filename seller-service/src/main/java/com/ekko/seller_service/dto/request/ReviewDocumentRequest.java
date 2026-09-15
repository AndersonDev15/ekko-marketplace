package com.ekko.seller_service.dto.request;

import com.ekko.seller_service.enums.DocumentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ReviewDocumentRequest(
        @Schema(description = "Document review status", example = "APPROVED", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull DocumentStatus status,

        @Schema(description = "Review notes", example = "Document verified successfully")
        String notes
) {}