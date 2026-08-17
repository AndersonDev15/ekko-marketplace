package com.ekko.payment_service.application.exception;

public class RefundNotFoundException extends RuntimeException {

    private final String stripeRefundId;

    public RefundNotFoundException(String stripeRefundId) {
        super("Refund not found for Stripe refund id " + stripeRefundId);
        this.stripeRefundId = stripeRefundId;
    }

    public String getStripeRefundId() {
        return stripeRefundId;
    }
}