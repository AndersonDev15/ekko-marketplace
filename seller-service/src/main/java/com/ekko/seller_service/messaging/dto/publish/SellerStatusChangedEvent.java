package com.ekko.seller_service.messaging.dto.publish;

import com.ekko.seller_service.enums.SellerStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SellerStatusChangedEvent(
        UUID sellerId,
        String keycloakId,
        String email,
        SellerStatus previousStatus,
        SellerStatus newStatus,
        LocalDateTime changedAt
) {
}