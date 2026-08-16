package com.ekko.seller_service.messaging.dto;

import com.ekko.seller_service.enums.SellerStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SellerStatusChangedEvent(
        UUID sellerId,
        String keycloakId,
        SellerStatus previousStatus,
        SellerStatus newStatus,
        LocalDateTime changedAt
) {
}