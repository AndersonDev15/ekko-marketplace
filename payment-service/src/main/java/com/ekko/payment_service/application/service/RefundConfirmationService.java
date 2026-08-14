package com.ekko.payment_service.application.service;

import com.ekko.payment_service.domain.exception.PaymentNotFoundException;
import com.ekko.payment_service.domain.exception.RefundNotFoundException;
import com.ekko.payment_service.domain.model.HandleRefundSucceededCommand;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.model.PaymentRefundedEvent;
import com.ekko.payment_service.domain.model.PaymentTransaction;
import com.ekko.payment_service.domain.model.Refund;
import com.ekko.payment_service.domain.model.RefundStatus;
import com.ekko.payment_service.domain.model.TransactionStatus;
import com.ekko.payment_service.domain.model.TransactionType;
import com.ekko.payment_service.domain.port.in.HandleRefundSucceededUseCase;
import com.ekko.payment_service.domain.port.out.PaymentEventPublisherPort;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.domain.port.out.PaymentTransactionRepositoryPort;
import com.ekko.payment_service.domain.port.out.RefundRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefundConfirmationService implements HandleRefundSucceededUseCase {

    private final RefundRepositoryPort refundRepositoryPort;
    private final PaymentRepositoryPort paymentRepositoryPort;
    private final PaymentTransactionRepositoryPort paymentTransactionRepositoryPort;
    private final PaymentEventPublisherPort paymentEventPublisherPort;
    private final RefundTransactionService refundTransactionService;

    @Override
    public Refund execute(HandleRefundSucceededCommand command) {
        if (paymentTransactionRepositoryPort.existsByStripeEventId(command.stripeEventId())) {
            return findRefund(command.stripeRefundId());
        }

        Refund refund = findRefund(command.stripeRefundId());
        if (isTerminal(refund.getStatus())) {
            return refund;
        }

        refund.markSucceeded();

        Payment payment = paymentRepositoryPort.findById(refund.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException(refund.getPaymentId()));

        BigDecimal alreadyRefunded = refundRepositoryPort.sumSucceededAmountByPaymentId(payment.getId());
        BigDecimal newTotalRefunded = alreadyRefunded.add(refund.getAmount());
        boolean isTotal = newTotalRefunded.compareTo(payment.getAmount()) == 0;
        if (isTotal) {
            payment.markRefunded();
        }

        PaymentTransaction transaction = PaymentTransaction.initiate(
                payment.getId(),
                TransactionType.REFUND,
                refund.getAmount(),
                payment.getCurrency(),
                TransactionStatus.SUCCEEDED,
                command.stripeEventId());

        refundTransactionService.confirmRefund(refund, payment, transaction);

        paymentEventPublisherPort.publishPaymentRefunded(toEvent(payment, refund, isTotal));
        return refund;
    }

    private Refund findRefund(String stripeRefundId) {
        return refundRepositoryPort.findByStripeRefundId(stripeRefundId)
                .orElseThrow(() -> new RefundNotFoundException(stripeRefundId));
    }

    private boolean isTerminal(RefundStatus status) {
        return status == RefundStatus.SUCCEEDED || status == RefundStatus.FAILED;
    }

    private PaymentRefundedEvent toEvent(Payment payment, Refund refund, boolean isTotal) {
        return new PaymentRefundedEvent(
                payment.getId(),
                payment.getOrderId(),
                payment.getCustomerId(),
                refund.getId(),
                refund.getAmount(),
                isTotal,
                LocalDateTime.now());
    }
}