package com.ekko.payment_service.application.service;

import com.ekko.payment_service.domain.exception.PaymentNotFoundException;
import com.ekko.payment_service.domain.model.HandlePaymentSucceededCommand;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.model.PaymentCompletedEvent;
import com.ekko.payment_service.domain.model.PaymentStatus;
import com.ekko.payment_service.domain.model.PaymentSucceededInternalEvent;
import com.ekko.payment_service.domain.model.PaymentTransaction;
import com.ekko.payment_service.domain.model.TransactionStatus;
import com.ekko.payment_service.domain.model.TransactionType;
import com.ekko.payment_service.domain.port.in.HandlePaymentSucceededUseCase;
import com.ekko.payment_service.domain.port.out.PaymentEventPublisherPort;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.domain.port.out.PaymentTransactionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentConfirmationService implements HandlePaymentSucceededUseCase {

    private final PaymentRepositoryPort paymentRepositoryPort;
    private final PaymentTransactionRepositoryPort paymentTransactionRepositoryPort;
    private final PaymentEventPublisherPort paymentEventPublisherPort;
    private final PaymentTransactionService paymentTransactionService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public Payment execute(HandlePaymentSucceededCommand command) {
        if (paymentTransactionRepositoryPort.existsByStripeEventId(command.stripeEventId())) {
            return findPayment(command.paymentIntentId());
        }

        Payment payment = findPayment(command.paymentIntentId());
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return payment;
        }

        payment.markSucceeded(
                command.paymentMethodType(),
                command.paymentMethodLast4(),
                command.paidAt());

        PaymentTransaction transaction = PaymentTransaction.initiate(
                payment.getId(),
                TransactionType.CHARGE,
                payment.getAmount(),
                payment.getCurrency(),
                TransactionStatus.SUCCEEDED,
                command.stripeEventId());

        Payment saved = paymentTransactionService.confirmPayment(payment, transaction);
        paymentEventPublisherPort.publishPaymentCompleted(toEvent(saved));
        applicationEventPublisher.publishEvent(new PaymentSucceededInternalEvent(saved.getId()));
        return saved;
    }

    private Payment findPayment(String paymentIntentId) {
        return paymentRepositoryPort.findByPaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentIntentId));
    }

    private PaymentCompletedEvent toEvent(Payment saved) {
        return new PaymentCompletedEvent(
                saved.getId(),
                saved.getOrderId(),
                saved.getCustomerId(),
                saved.getAmount(),
                saved.getCurrency(),
                saved.getPaidAt());
    }
}