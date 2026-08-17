package com.ekko.payment_service.application.service;

import com.ekko.payment_service.application.exception.PaymentNotFoundException;
import com.ekko.payment_service.domain.command.HandlePaymentCancelledCommand;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.event.PaymentCancelledEvent;
import com.ekko.payment_service.domain.enums.PaymentStatus;
import com.ekko.payment_service.domain.model.PaymentTransaction;
import com.ekko.payment_service.domain.enums.TransactionStatus;
import com.ekko.payment_service.domain.enums.TransactionType;
import com.ekko.payment_service.domain.port.in.HandlePaymentCancelledUseCase;
import com.ekko.payment_service.domain.port.out.PaymentEventPublisherPort;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.domain.port.out.PaymentTransactionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentCancellationService implements HandlePaymentCancelledUseCase {

    private final PaymentRepositoryPort paymentRepositoryPort;
    private final PaymentTransactionRepositoryPort paymentTransactionRepositoryPort;
    private final PaymentEventPublisherPort paymentEventPublisherPort;
    private final PaymentTransactionService paymentTransactionService;

    @Override
    public Payment execute(HandlePaymentCancelledCommand command) {
        if (paymentTransactionRepositoryPort.existsByStripeEventId(command.stripeEventId())) {
            return findPayment(command.paymentIntentId());
        }

        Payment payment = findPayment(command.paymentIntentId());
        if (!canBeCancelled(payment.getStatus())) {
            return payment;
        }

        payment.markCancelled();

        PaymentTransaction transaction = PaymentTransaction.initiate(
                payment.getId(),
                TransactionType.VOID,
                payment.getAmount(),
                payment.getCurrency(),
                TransactionStatus.SUCCEEDED,
                command.stripeEventId(),
                null);

        Payment saved = paymentTransactionService.cancelPayment(payment, transaction);
        paymentEventPublisherPort.publishPaymentCancelled(toEvent(saved));
        return saved;
    }

    private Payment findPayment(String paymentIntentId) {
        return paymentRepositoryPort.findByPaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentIntentId));
    }

    private boolean canBeCancelled(PaymentStatus status) {
        return status == PaymentStatus.PENDING || status == PaymentStatus.PROCESSING;
    }

    private PaymentCancelledEvent toEvent(Payment saved) {
        return new PaymentCancelledEvent(
                saved.getId(),
                saved.getOrderId(),
                saved.getCustomerId(),
                LocalDateTime.now());
    }
}