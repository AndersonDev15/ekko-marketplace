package com.ekko.notification_service.dto.request;

import com.ekko.notification_service.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to create a notification template")
public record CreateTemplateRequest(
        @NotBlank
        @Schema(description = "Template name (unique per type)", example = "welcome_email", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @NotNull
        @Schema(description = "Notification type", example = "EMAIL", requiredMode = Schema.RequiredMode.REQUIRED)
        NotificationType type,

        @Schema(description = "Email subject template (optional for IN_APP)", example = "Welcome {{name}}!")
        String subject,

        @NotBlank
        @Schema(description = "Template body content with variables", example = "Hello {{name}}, welcome to {{platform}}!", requiredMode = Schema.RequiredMode.REQUIRED)
        String body,

        @Schema(description = "JSON schema of template variables", example = "{\"name\": \"string\", \"platform\": \"string\"}")
        String variables
) {}