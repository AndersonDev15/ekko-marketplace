package com.ekko.notification_service.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Notification delivery type")
public enum NotificationType {
    EMAIL,
    IN_APP
}