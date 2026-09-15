package com.ekko.notification_service.dto.response;

import com.ekko.notification_service.enums.NotificationStatus;
import com.ekko.notification_service.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Notification response")
public record NotificationResponse(
        @Schema(description = "Notification ID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "Notification type")
        NotificationType type,

        @Schema(description = "Notification subject", example = "Welcome to Ekko!")
        String subject,

        @Schema(description = "Notification body content", example = "Hello, welcome to our platform...")
        String body,

        @Schema(description = "Notification status")
        NotificationStatus status,

        @Schema(description = "Timestamp when notification was read", example = "2024-01-15T10:30:00")
        LocalDateTime readAt,

        @Schema(description = "Timestamp when notification was sent", example = "2024-01-15T10:00:00")
        LocalDateTime sentAt,

        @Schema(description = "Timestamp when notification was created", example = "2024-01-15T10:00:00")
        LocalDateTime createdAt
) {}