package com.ekko.payment_service.domain.command;

public record HandleRefundSucceededCommand(
        String stripeRefundId,
        String stripeEventId
) {
}