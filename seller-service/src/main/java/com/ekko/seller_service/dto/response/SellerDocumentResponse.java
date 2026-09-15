package com.ekko.seller_service.dto.response;

import com.ekko.seller_service.enums.DocumentStatus;
import com.ekko.seller_service.enums.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record SellerDocumentResponse(
        @Schema(description = "Document UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Document type", example = "ID_CARD")
        DocumentType documentType,

        @Schema(description = "Document status", example = "PENDING")
        DocumentStatus status,

        @Schema(description = "Upload timestamp", example = "2024-01-15T10:30:00")
        LocalDateTime uploadedAt,

        @Schema(description = "Review timestamp", example = "2024-01-20T14:45:00")
        LocalDateTime reviewedAt,

        @Schema(description = "Review notes", example = "Document verified successfully")
        String notes
) {}