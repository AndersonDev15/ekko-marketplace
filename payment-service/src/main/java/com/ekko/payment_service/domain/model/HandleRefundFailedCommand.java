package com.ekko.payment_service.domain.model;

public record HandleRefundFailedCommand(
        String stripeRefundId,
        String stripeEventId
) {
}