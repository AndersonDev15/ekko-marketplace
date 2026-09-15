package com.ekko.notification_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Unread notification count response")
public record UnreadCountResponse(
        @Schema(description = "Number of unread notifications", example = "5")
        long count
) {}