package com.ekko.payment_service.application.service;

import com.ekko.payment_service.domain.exception.PaymentNotFoundException;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.model.PaymentTransaction;
import com.ekko.payment_service.domain.policy.PaymentOwnershipPolicy;
import com.ekko.payment_service.domain.port.in.GetTransactionsByPaymentIdForAdminUseCase;
import com.ekko.payment_service.domain.port.in.GetTransactionsByPaymentIdUseCase;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.domain.port.out.PaymentTransactionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentTransactionsQueryService implements
        GetTransactionsByPaymentIdUseCase,
        GetTransactionsByPaymentIdForAdminUseCase {

    private final PaymentRepositoryPort paymentRepositoryPort;
    private final PaymentTransactionRepositoryPort paymentTransactionRepositoryPort;
    private final PaymentOwnershipPolicy paymentOwnershipPolicy;

    @Override
    public List<PaymentTransaction> execute(UUID paymentId, UUID keycloakId, String guestEmail) {
        Payment payment = findPayment(paymentId);
        assertOwned(payment, keycloakId, guestEmail);
        return paymentTransactionRepositoryPort.findByPaymentId(paymentId);
    }

    @Override
    public List<PaymentTransaction> execute(UUID paymentId) {
        findPayment(paymentId);
        return paymentTransactionRepositoryPort.findByPaymentId(paymentId);
    }

    private void assertOwned(Payment payment, UUID keycloakId, String guestEmail) {
        if (keycloakId != null) {
            paymentOwnershipPolicy.assertCustomerOwns(payment, keycloakId);
        } else {
            paymentOwnershipPolicy.assertGuestOwns(payment, guestEmail);
        }
    }

    private Payment findPayment(UUID paymentId) {
        return paymentRepositoryPort.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }
}