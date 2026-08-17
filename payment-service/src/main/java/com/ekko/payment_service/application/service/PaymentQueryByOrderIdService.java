package com.ekko.payment_service.application.service;

import com.ekko.payment_service.application.exception.PaymentNotFoundException;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.policy.PaymentOwnershipPolicy;
import com.ekko.payment_service.domain.port.in.GetPaymentByOrderIdForAdminUseCase;
import com.ekko.payment_service.domain.port.in.GetPaymentByOrderIdUseCase;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentQueryByOrderIdService implements
        GetPaymentByOrderIdUseCase,
        GetPaymentByOrderIdForAdminUseCase {

    private final PaymentRepositoryPort paymentRepositoryPort;
    private final PaymentOwnershipPolicy paymentOwnershipPolicy;

    @Override
    public Payment execute(UUID orderId, UUID keycloakId, String guestEmail) {
        Payment payment = findPaymentByOrderId(orderId);
        assertOwned(payment, keycloakId, guestEmail);
        return payment;
    }

    @Override
    public Payment execute(UUID orderId) {
        return findPaymentByOrderId(orderId);
    }

    private void assertOwned(Payment payment, UUID keycloakId, String guestEmail) {
        if (keycloakId != null) {
            paymentOwnershipPolicy.assertCustomerOwns(payment, keycloakId);
        } else {
            paymentOwnershipPolicy.assertGuestOwns(payment, guestEmail);
        }
    }

    private Payment findPaymentByOrderId(UUID orderId) {
        return paymentRepositoryPort.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(orderId));
    }
}