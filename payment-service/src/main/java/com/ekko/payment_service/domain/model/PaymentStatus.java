package com.ekko.payment_service.domain.model;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    REFUNDED,
    DISPUTED
}
