package com.ekko.payment_service.domain.model;

public enum PaymentFailureReason {
    DECLINED,
    FRAUD,
    TIMEOUT,
    ERROR
}