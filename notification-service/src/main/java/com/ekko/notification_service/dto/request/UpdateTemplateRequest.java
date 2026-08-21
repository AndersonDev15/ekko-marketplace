package com.ekko.notification_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateTemplateRequest(
        String subject,
        @NotBlank
        String body,
        String variables
) {}