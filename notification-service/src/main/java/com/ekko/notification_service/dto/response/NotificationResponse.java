package com.ekko.notification_service.dto.response;

import com.ekko.notification_service.enums.NotificationStatus;
import com.ekko.notification_service.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String subject,
        String body,
        NotificationStatus status,
        LocalDateTime readAt,
        LocalDateTime sentAt,
        LocalDateTime createdAt
) {}