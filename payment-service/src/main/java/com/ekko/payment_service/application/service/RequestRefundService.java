package com.ekko.payment_service.application.service;

import com.ekko.payment_service.application.exception.PaymentNotFoundException;
import com.ekko.payment_service.domain.command.CreateRefundCommand;
import com.ekko.payment_service.domain.enums.PaymentStatus;
import com.ekko.payment_service.domain.exception.RefundAmountExceededException;
import com.ekko.payment_service.domain.exception.RefundNotAllowedException;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.model.Refund;
import com.ekko.payment_service.domain.policy.PaymentOwnershipPolicy;
import com.ekko.payment_service.domain.port.in.RequestRefundUseCase;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.domain.port.out.RefundRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RequestRefundService implements RequestRefundUseCase {

    private final PaymentRepositoryPort paymentRepositoryPort;
    private final RefundRepositoryPort refundRepositoryPort;
    private final PaymentOwnershipPolicy paymentOwnershipPolicy;

    @Override
    public Refund execute(CreateRefundCommand command) {
        Payment payment = paymentRepositoryPort.findById(command.paymentId())
                .orElseThrow(() -> new PaymentNotFoundException(command.paymentId()));

        assertOwned(payment, command.requestedBy(), command.guestEmail());
        assertRefundAllowed(payment);
        assertAmountWithinLimit(payment, command.amount());

        Refund refund = Refund.initiate(
                command.paymentId(),
                command.amount(),
                command.reason(),
                command.requestedBy(),
                command.notes());

        return refundRepositoryPort.save(refund);
    }

    private void assertOwned(Payment payment, UUID keycloakId, String guestEmail) {
        if (keycloakId != null) {
            paymentOwnershipPolicy.assertCustomerOwns(payment, keycloakId);
        } else {
            paymentOwnershipPolicy.assertGuestOwns(payment, guestEmail);
        }
    }

    private void assertRefundAllowed(Payment payment) {
        if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
            throw new RefundNotAllowedException(payment.getId(), payment.getStatus());
        }
    }

    private void assertAmountWithinLimit(Payment payment, BigDecimal requestedAmount) {
        BigDecimal alreadyRefunded = refundRepositoryPort
                .sumSucceededAmountByPaymentId(payment.getId());
        BigDecimal totalAfterRefund = alreadyRefunded.add(requestedAmount);
        if (totalAfterRefund.compareTo(payment.getAmount()) > 0) {
            throw new RefundAmountExceededException(
                    payment.getId(), requestedAmount, alreadyRefunded, payment.getAmount());
        }
    }
}