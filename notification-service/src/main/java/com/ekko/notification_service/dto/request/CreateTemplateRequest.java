package com.ekko.notification_service.dto.request;

import com.ekko.notification_service.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTemplateRequest(
        @NotBlank
        String name,
        @NotNull
        NotificationType type,
        String subject,
        @NotBlank
        String body,
        String variables
) {}