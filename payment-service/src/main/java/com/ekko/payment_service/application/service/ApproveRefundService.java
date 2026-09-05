package com.ekko.payment_service.application.service;

import com.ekko.payment_service.application.exception.PaymentNotFoundException;
import com.ekko.payment_service.application.exception.RefundAlreadyProcessedException;
import com.ekko.payment_service.application.exception.RefundNotFoundException;
import com.ekko.payment_service.domain.enums.RefundStatus;
import com.ekko.payment_service.domain.exception.RefundAmountExceededException;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.model.Refund;
import com.ekko.payment_service.domain.port.in.ApproveRefundUseCase;
import com.ekko.payment_service.domain.port.out.PaymentGatewayPort;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.domain.port.out.RefundRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApproveRefundService implements ApproveRefundUseCase {

    private final RefundRepositoryPort refundRepositoryPort;
    private final PaymentRepositoryPort paymentRepositoryPort;
    private final PaymentGatewayPort paymentGatewayPort;

    @Override
    public Refund execute(UUID refundId) {
        Refund refund = refundRepositoryPort.findById(refundId)
                .orElseThrow(() -> new RefundNotFoundException(refundId));

        assertPending(refund);

        Payment payment = paymentRepositoryPort.findById(refund.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException(refund.getPaymentId()));

        assertAmountStillWithinLimit(payment, refund);

        String stripeRefundId = paymentGatewayPort.createRefund(
                payment.getPaymentIntentId(),
                refund.getAmount(),
                refund.getReason());

        refund.attachStripeRefundId(stripeRefundId);

        return refundRepositoryPort.save(refund);
    }

    private void assertPending(Refund refund) {
        if (refund.getStatus() != RefundStatus.PENDING || refund.getStripeRefundId() != null) {
            throw new RefundAlreadyProcessedException(refund.getId(), refund.getStatus());
        }
    }

    private void assertAmountStillWithinLimit(Payment payment, Refund refund) {
        BigDecimal alreadyRefunded = refundRepositoryPort
                .sumSucceededAmountByPaymentId(payment.getId());
        BigDecimal totalAfterRefund = alreadyRefunded.add(refund.getAmount());
        if (totalAfterRefund.compareTo(payment.getAmount()) > 0) {
            throw new RefundAmountExceededException(
                    payment.getId(), refund.getAmount(), alreadyRefunded, payment.getAmount());
        }
    }
}
