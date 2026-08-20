package com.ekko.notification_service.dto.response;

import com.ekko.notification_service.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record TemplateResponse(
        UUID id,
        String name,
        NotificationType type,
        String subject,
        String body,
        String variables,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}