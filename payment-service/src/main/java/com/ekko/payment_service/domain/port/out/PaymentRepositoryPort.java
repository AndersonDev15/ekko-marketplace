package com.ekko.payment_service.domain.port.out;

import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.model.PaymentStatus;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepositoryPort {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID id);

    Optional<Payment> findByOrderIdAndStatus(UUID orderId, PaymentStatus status);

    Optional<Payment> findByPaymentIntentId(String paymentIntentId);

    Optional<String> findExistingStripeCustomerId(UUID customerId);
}
