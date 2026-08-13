package com.ekko.payment_service.domain.port.out;

import com.ekko.payment_service.domain.model.PaymentTransaction;

public interface PaymentTransactionRepositoryPort {

    PaymentTransaction save(PaymentTransaction transaction);

    boolean existsByStripeEventId(String stripeEventId);
}