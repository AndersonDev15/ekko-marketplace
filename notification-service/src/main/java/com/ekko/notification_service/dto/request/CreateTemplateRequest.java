package com.ekko.notification_service.dto.request;

import com.ekko.notification_service.enums.NotificationType;

public record CreateTemplateRequest(
        String name,
        NotificationType type,
        String subject,
        String body,
        String variables
) {}