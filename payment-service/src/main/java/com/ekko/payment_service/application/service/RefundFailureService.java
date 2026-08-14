package com.ekko.payment_service.application.service;

import com.ekko.payment_service.domain.exception.PaymentNotFoundException;
import com.ekko.payment_service.domain.exception.RefundNotFoundException;
import com.ekko.payment_service.domain.model.HandleRefundFailedCommand;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.model.PaymentRefundFailedEvent;
import com.ekko.payment_service.domain.model.PaymentTransaction;
import com.ekko.payment_service.domain.model.Refund;
import com.ekko.payment_service.domain.model.RefundStatus;
import com.ekko.payment_service.domain.model.TransactionStatus;
import com.ekko.payment_service.domain.model.TransactionType;
import com.ekko.payment_service.domain.port.in.HandleRefundFailedUseCase;
import com.ekko.payment_service.domain.port.out.PaymentEventPublisherPort;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.domain.port.out.PaymentTransactionRepositoryPort;
import com.ekko.payment_service.domain.port.out.RefundRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefundFailureService implements HandleRefundFailedUseCase {

    private final RefundRepositoryPort refundRepositoryPort;
    private final PaymentRepositoryPort paymentRepositoryPort;
    private final PaymentTransactionRepositoryPort paymentTransactionRepositoryPort;
    private final PaymentEventPublisherPort paymentEventPublisherPort;
    private final RefundTransactionService refundTransactionService;

    @Override
    public Refund execute(HandleRefundFailedCommand command) {
        if (paymentTransactionRepositoryPort.existsByStripeEventId(command.stripeEventId())) {
            return findRefund(command.stripeRefundId());
        }

        Refund refund = findRefund(command.stripeRefundId());
        if (isTerminal(refund.getStatus())) {
            return refund;
        }

        refund.markFailed();

        Payment payment = paymentRepositoryPort.findById(refund.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException(refund.getPaymentId()));

        PaymentTransaction transaction = PaymentTransaction.initiate(
                payment.getId(),
                TransactionType.REFUND,
                refund.getAmount(),
                payment.getCurrency(),
                TransactionStatus.FAILED,
                command.stripeEventId());

        refundTransactionService.failRefund(refund, transaction);

        paymentEventPublisherPort.publishPaymentRefundFailed(toEvent(payment, refund));
        return refund;
    }

    private Refund findRefund(String stripeRefundId) {
        return refundRepositoryPort.findByStripeRefundId(stripeRefundId)
                .orElseThrow(() -> new RefundNotFoundException(stripeRefundId));
    }

    private boolean isTerminal(RefundStatus status) {
        return status == RefundStatus.SUCCEEDED || status == RefundStatus.FAILED;
    }

    private PaymentRefundFailedEvent toEvent(Payment payment, Refund refund) {
        return new PaymentRefundFailedEvent(
                payment.getId(),
                payment.getOrderId(),
                refund.getId(),
                LocalDateTime.now());
    }
}