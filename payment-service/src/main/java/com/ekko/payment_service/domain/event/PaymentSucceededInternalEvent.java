package com.ekko.payment_service.domain.event;

import java.util.UUID;

public record PaymentSucceededInternalEvent(
        UUID paymentId
) {
}