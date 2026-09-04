package com.ekko.payment_service.infrastructure.persistence.adapter.in.web.dto.response;

import com.ekko.payment_service.domain.enums.PaymentStatus;

import java.util.UUID;

public record PaymentCheckoutStatusResponse(
        UUID orderId,
        PaymentStatus status,
        String clientSecret // null salvo que status == PENDING
) {
}