package com.ekko.payment_service.domain.command;

public record HandleRefundFailedCommand(
        String stripeRefundId,
        String stripeEventId
) {
}