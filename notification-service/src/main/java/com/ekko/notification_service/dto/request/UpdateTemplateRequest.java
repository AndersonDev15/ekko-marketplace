package com.ekko.notification_service.dto.request;

public record UpdateTemplateRequest(
        String subject,
        String body,
        String variables
) {}