package com.ekko.payment_service.domain.port.out;

import com.ekko.payment_service.domain.model.PaymentTransaction;

import java.util.List;
import java.util.UUID;

public interface PaymentTransactionRepositoryPort {

    PaymentTransaction save(PaymentTransaction transaction);

    boolean existsByStripeEventId(String stripeEventId);

    List<PaymentTransaction> findByPaymentId(UUID paymentId);
}