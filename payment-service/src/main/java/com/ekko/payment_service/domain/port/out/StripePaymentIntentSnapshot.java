package com.ekko.payment_service.domain.port.out;

public record StripePaymentIntentSnapshot(String status, String clientSecret) {
}