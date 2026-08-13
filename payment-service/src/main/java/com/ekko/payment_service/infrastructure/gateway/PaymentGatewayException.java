package com.ekko.payment_service.infrastructure.gateway;

import com.stripe.exception.StripeException;

public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String message, StripeException cause) {
        super(message, cause);
    }

    public PaymentGatewayException(String message) {
        super(message);
    }
}
