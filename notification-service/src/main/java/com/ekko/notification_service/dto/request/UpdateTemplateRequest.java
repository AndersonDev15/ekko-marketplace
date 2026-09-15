package com.ekko.notification_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to update a notification template")
public record UpdateTemplateRequest(
        @Schema(description = "Email subject template (optional for IN_APP)", example = "Welcome {{name}}!")
        String subject,

        @NotBlank
        @Schema(description = "Template body content with variables", example = "Hello {{name}}, welcome to {{platform}}!", requiredMode = Schema.RequiredMode.REQUIRED)
        String body,

        @Schema(description = "JSON schema of template variables", example = "{\"name\": \"string\", \"platform\": \"string\"}")
        String variables
) {}