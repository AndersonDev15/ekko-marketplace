package com.ekko.seller_service.dto.response;

import java.time.Instant;
import java.util.UUID;

public record DocumentDownloadResponse(
        UUID documentId,
        String downloadUrl,
        Instant expiresAt
) {}