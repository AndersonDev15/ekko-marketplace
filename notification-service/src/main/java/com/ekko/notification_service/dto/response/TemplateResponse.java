package com.ekko.notification_service.dto.response;

import com.ekko.notification_service.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Notification template response")
public record TemplateResponse(
        @Schema(description = "Template ID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "Template name", example = "welcome_email")
        String name,

        @Schema(description = "Notification type")
        NotificationType type,

        @Schema(description = "Email subject template (can contain variables)", example = "Welcome {{name}}!")
        String subject,

        @Schema(description = "Template body content (can contain variables)", example = "Hello {{name}}, welcome to {{platform}}!")
        String body,

        @Schema(description = "JSON schema of template variables", example = "{\"name\": \"string\", \"platform\": \"string\"}")
        String variables,

        @Schema(description = "Whether template is active", example = "true")
        Boolean isActive,

        @Schema(description = "Creation timestamp", example = "2024-01-15T10:00:00")
        LocalDateTime createdAt,

        @Schema(description = "Last update timestamp", example = "2024-01-15T10:00:00")
        LocalDateTime updatedAt
) {}