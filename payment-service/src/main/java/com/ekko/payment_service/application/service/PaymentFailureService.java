package com.ekko.payment_service.application.service;

import com.ekko.payment_service.application.exception.PaymentNotFoundException;
import com.ekko.payment_service.domain.command.HandlePaymentFailedCommand;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.event.PaymentFailedEvent;
import com.ekko.payment_service.domain.enums.PaymentStatus;
import com.ekko.payment_service.domain.model.PaymentTransaction;
import com.ekko.payment_service.domain.enums.TransactionStatus;
import com.ekko.payment_service.domain.enums.TransactionType;
import com.ekko.payment_service.domain.port.in.HandlePaymentFailedUseCase;
import com.ekko.payment_service.domain.port.out.PaymentEventPublisherPort;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.domain.port.out.PaymentTransactionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentFailureService implements HandlePaymentFailedUseCase {

    private final PaymentRepositoryPort paymentRepositoryPort;
    private final PaymentTransactionRepositoryPort paymentTransactionRepositoryPort;
    private final PaymentEventPublisherPort paymentEventPublisherPort;
    private final PaymentTransactionService paymentTransactionService;

    @Override
    public Payment execute(HandlePaymentFailedCommand command) {
        if (paymentTransactionRepositoryPort.existsByStripeEventId(command.stripeEventId())) {
            return findPayment(command.paymentIntentId());
        }

        Payment payment = findPayment(command.paymentIntentId());
        if (isTerminal(payment.getStatus())) {
            return payment;
        }

        payment.markFailed();

        PaymentTransaction transaction = PaymentTransaction.initiate(
                payment.getId(),
                TransactionType.CHARGE,
                payment.getAmount(),
                payment.getCurrency(),
                TransactionStatus.FAILED,
                command.stripeEventId(),
                command.errorMessage());

        Payment saved = paymentTransactionService.failPayment(payment, transaction);
        paymentEventPublisherPort.publishPaymentFailed(toEvent(saved, command));
        return saved;
    }

    private Payment findPayment(String paymentIntentId) {
        return paymentRepositoryPort.findByPaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentIntentId));
    }

    private boolean isTerminal(PaymentStatus status) {
        return status == PaymentStatus.SUCCEEDED || status == PaymentStatus.FAILED;
    }

    private PaymentFailedEvent toEvent(Payment saved, HandlePaymentFailedCommand command) {
        return new PaymentFailedEvent(
                saved.getId(),
                saved.getOrderId(),
                saved.getCustomerId(),
                saved.getGuestEmail(),
                command.reason(),
                LocalDateTime.now());
    }
}