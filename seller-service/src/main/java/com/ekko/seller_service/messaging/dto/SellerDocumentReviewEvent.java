package com.ekko.seller_service.messaging.dto;

import com.ekko.seller_service.enums.DocumentStatus;
import com.ekko.seller_service.enums.DocumentType;

import java.time.LocalDateTime;
import java.util.UUID;

public record SellerDocumentReviewEvent(
        UUID sellerId,
        UUID documentId,
        DocumentType documentType,
        DocumentStatus reviewStatus,
        LocalDateTime reviewedAt,
        String notes
) {
}