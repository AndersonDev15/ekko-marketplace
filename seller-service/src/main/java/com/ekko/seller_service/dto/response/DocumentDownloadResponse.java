package com.ekko.seller_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record DocumentDownloadResponse(
        @Schema(description = "Document UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID documentId,

        @Schema(description = "Presigned download URL", example = "https://minio.example.com/bucket/doc123?signature=abc")
        String downloadUrl,

        @Schema(description = "URL expiration timestamp", example = "2024-01-15T11:30:00Z")
        Instant expiresAt
) {}