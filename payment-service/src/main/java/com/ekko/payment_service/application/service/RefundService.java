package com.ekko.payment_service.application.service;

import com.ekko.payment_service.domain.exception.PaymentNotFoundException;
import com.ekko.payment_service.domain.exception.RefundAmountExceededException;
import com.ekko.payment_service.domain.exception.RefundNotAllowedException;
import com.ekko.payment_service.domain.model.CreateRefundCommand;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.model.PaymentStatus;
import com.ekko.payment_service.domain.model.Refund;
import com.ekko.payment_service.domain.port.in.CreateRefundUseCase;
import com.ekko.payment_service.domain.port.out.PaymentGatewayPort;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.domain.port.out.RefundRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class RefundService implements CreateRefundUseCase {

    private final PaymentRepositoryPort paymentRepositoryPort;
    private final RefundRepositoryPort refundRepositoryPort;
    private final PaymentGatewayPort paymentGatewayPort;
    private final RefundTransactionService refundTransactionService;

    @Override
    public Refund execute(CreateRefundCommand command) {
        Payment payment = paymentRepositoryPort.findById(command.paymentId())
                .orElseThrow(() -> new PaymentNotFoundException(command.paymentId().toString()));

        assertRefundAllowed(payment);
        assertAmountWithinLimit(payment, command.amount());

        Refund refund = Refund.initiate(
                command.paymentId(),
                command.amount(),
                command.reason(),
                command.requestedBy(),
                command.notes());

        String stripeRefundId = paymentGatewayPort.createRefund(
                payment.getPaymentIntentId(),
                refund.getAmount(),
                refund.getReason());

        refund.attachStripeRefundId(stripeRefundId);
        return refundTransactionService.commitRefund(refund);
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
                    payment.getId(),
                    requestedAmount,
                    alreadyRefunded,
                    payment.getAmount());
        }
    }
}