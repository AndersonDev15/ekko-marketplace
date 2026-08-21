package com.ekko.notification_service.messaging.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record SellerDocumentReviewEvent(
        UUID sellerId,
        String email,
        String keycloakId,
        UUID documentId,
        DocumentType documentType,
        DocumentStatus reviewStatus,
        LocalDateTime reviewedAt,
        String notes
) {
}