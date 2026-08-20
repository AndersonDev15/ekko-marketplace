package com.ekko.notification_service.messaging.dto;

import com.ekko.notification_service.enums.NotificationType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record NotificationEvent(
        UUID recipientId,
        String recipientEmail,
        String eventType,
        List<NotificationType> channels,
        Map<String, Object> variables
) {}