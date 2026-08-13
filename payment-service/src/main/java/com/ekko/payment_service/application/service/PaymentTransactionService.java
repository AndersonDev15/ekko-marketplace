package com.ekko.payment_service.application.service;

import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.model.PaymentTransaction;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.domain.port.out.PaymentTransactionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

    private final PaymentRepositoryPort paymentRepositoryPort;
    private final PaymentTransactionRepositoryPort paymentTransactionRepositoryPort;

    @Transactional
    public Payment commitPayment(Payment payment) {
        return paymentRepositoryPort.save(payment);
    }

    @Transactional
    public Payment confirmPayment(Payment payment, PaymentTransaction transaction) {
        Payment saved = paymentRepositoryPort.save(payment);
        paymentTransactionRepositoryPort.save(transaction);
        return saved;
    }

    @Transactional
    public Payment failPayment(Payment payment, PaymentTransaction transaction) {
        Payment saved = paymentRepositoryPort.save(payment);
        paymentTransactionRepositoryPort.save(transaction);
        return saved;
    }

    @Transactional
    public Payment cancelPayment(Payment payment, PaymentTransaction transaction) {
        Payment saved = paymentRepositoryPort.save(payment);
        paymentTransactionRepositoryPort.save(transaction);
        return saved;
    }
}
