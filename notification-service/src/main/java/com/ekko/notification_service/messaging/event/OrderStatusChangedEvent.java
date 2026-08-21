package com.ekko.notification_service.messaging.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderStatusChangedEvent(
        UUID orderId,
        String orderNumber,
        UUID customerId,
        String customerEmail,
        OrderStatus previousStatus,
        OrderStatus newStatus,
        List<UUID> sellerKeycloakIds,
        LocalDateTime changedAt
) {
}