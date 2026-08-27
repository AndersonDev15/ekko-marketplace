package com.ekko.product_service.messaging.dto.consume;

import com.ekko.product_service.enums.SellerStatus;

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
