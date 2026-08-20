package com.ekko.seller_service.messaging.dto.publish;

import com.ekko.seller_service.enums.DocumentStatus;
import com.ekko.seller_service.enums.DocumentType;

import java.time.LocalDateTime;
import java.util.UUID;

public record SellerDocumentReviewEvent(
        UUID sellerId,
        String email,
        UUID documentId,
        DocumentType documentType,
        DocumentStatus reviewStatus,
        LocalDateTime reviewedAt,
        String notes
) {
}