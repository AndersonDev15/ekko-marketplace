package com.ekko.payment_service.domain.port.out;

import com.ekko.payment_service.domain.enums.RefundReason;

import java.math.BigDecimal;

public interface PaymentGatewayPort {

    String resolveOrCreateCustomer(String existingStripeCustomerId, String email);

    String createPaymentIntent(BigDecimal amount, String currency, String stripeCustomerId, String transferGroup);

    String createTransfer(BigDecimal amount, String currency, String stripeAccountId, String transferGroup);

    String createRefund(String paymentIntentId, BigDecimal amount, RefundReason reason);

    String createConnectedAccount(String country, String email);

    String createAccountLink(String stripeAccountId, String refreshUrl, String returnUrl);


    String  retrievePaymentIntentClientSecret(String paymentIntentId);

    StripePaymentIntentSnapshot retrievePaymentIntent(String paymentIntentId);
}