package com.ekko.payment_service.domain.model;

public record HandleRefundSucceededCommand(
        String stripeRefundId,
        String stripeEventId
) {
}