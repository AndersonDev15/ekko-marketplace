package com.ekko.payment_service.domain.model;

import java.util.UUID;

public record PaymentSucceededInternalEvent(
        UUID paymentId
) {
}