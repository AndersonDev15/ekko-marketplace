package com.ekko.notification_service.messaging.event;

public enum PaymentFailureReason {
    DECLINED,
    FRAUD,
    TIMEOUT,
    ERROR
}