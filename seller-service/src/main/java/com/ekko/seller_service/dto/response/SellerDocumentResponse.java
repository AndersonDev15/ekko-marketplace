package com.ekko.seller_service.dto.response;

import com.ekko.seller_service.enums.DocumentStatus;
import com.ekko.seller_service.enums.DocumentType;

import java.time.LocalDateTime;
import java.util.UUID;

public record SellerDocumentResponse(
        UUID id,
        DocumentType documentType,
        DocumentStatus status,
        LocalDateTime uploadedAt,
        LocalDateTime reviewedAt,
        String notes
) {}