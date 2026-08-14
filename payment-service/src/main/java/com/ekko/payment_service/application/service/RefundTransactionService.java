package com.ekko.payment_service.application.service;

import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.model.PaymentTransaction;
import com.ekko.payment_service.domain.model.Refund;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.domain.port.out.PaymentTransactionRepositoryPort;
import com.ekko.payment_service.domain.port.out.RefundRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefundTransactionService {

    private final RefundRepositoryPort refundRepositoryPort;
    private final PaymentRepositoryPort paymentRepositoryPort;
    private final PaymentTransactionRepositoryPort paymentTransactionRepositoryPort;

    @Transactional
    public Refund commitRefund(Refund refund) {
        return refundRepositoryPort.save(refund);
    }

    @Transactional
    public Refund confirmRefund(Refund refund, Payment payment, PaymentTransaction transaction) {
        refundRepositoryPort.save(refund);
        paymentRepositoryPort.save(payment);
        paymentTransactionRepositoryPort.save(transaction);
        return refund;
    }

    @Transactional
    public Refund failRefund(Refund refund, PaymentTransaction transaction) {
        refundRepositoryPort.save(refund);
        paymentTransactionRepositoryPort.save(transaction);
        return refund;
    }
}