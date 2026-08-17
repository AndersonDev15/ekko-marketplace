package com.ekko.payment_service.application.service;

import com.ekko.payment_service.application.exception.PaymentNotFoundException;
import com.ekko.payment_service.domain.command.HandlePaymentSucceededCommand;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.event.PaymentCompletedEvent;
import com.ekko.payment_service.domain.enums.PaymentStatus;
import com.ekko.payment_service.domain.event.PaymentSucceededInternalEvent;
import com.ekko.payment_service.domain.model.PaymentTransaction;
import com.ekko.payment_service.domain.enums.TransactionStatus;
import com.ekko.payment_service.domain.enums.TransactionType;
import com.ekko.payment_service.domain.port.out.PaymentEventPublisherPort;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.domain.port.out.PaymentTransactionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentConfirmationServiceTest {

    private static final String PAYMENT_INTENT_ID = "pi_test_123";
    private static final String STRIPE_EVENT_ID = "evt_test_123";
    private static final LocalDateTime PAID_AT = LocalDateTime.of(2026, 8, 11, 10, 30, 0);

    @Mock
    private PaymentRepositoryPort paymentRepositoryPort;
    @Mock
    private PaymentTransactionRepositoryPort paymentTransactionRepositoryPort;
    @Mock
    private PaymentEventPublisherPort paymentEventPublisherPort;
    @Mock
    private PaymentTransactionService paymentTransactionService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private PaymentConfirmationService service;

    @BeforeEach
    void setUp() {
        service = new PaymentConfirmationService(
                paymentRepositoryPort,
                paymentTransactionRepositoryPort,
                paymentEventPublisherPort,
                paymentTransactionService,
                applicationEventPublisher);
    }

    @Test
    void confirmsPendingPaymentPublishesEventAndInternalEvent() {
        Payment payment = pendingPayment();
        when(paymentTransactionRepositoryPort.existsByStripeEventId(STRIPE_EVENT_ID)).thenReturn(false);
        when(paymentRepositoryPort.findByPaymentIntentId(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(payment));
        when(paymentTransactionService.confirmPayment(any(Payment.class), any(PaymentTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Payment result = service.execute(command());

        assertEquals(PaymentStatus.SUCCEEDED, result.getStatus());
        assertEquals(PAID_AT, result.getPaidAt());
        assertEquals("card", result.getPaymentMethodType());
        assertEquals("4242", result.getPaymentMethodLast4());

        ArgumentCaptor<PaymentTransaction> transactionCaptor =
                ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionService).confirmPayment(eq(payment), transactionCaptor.capture());
        assertEquals(TransactionType.CHARGE, transactionCaptor.getValue().getType());
        assertEquals(TransactionStatus.SUCCEEDED, transactionCaptor.getValue().getStatus());
        assertEquals(payment.getId(), transactionCaptor.getValue().getPaymentId());
        assertEquals(STRIPE_EVENT_ID, transactionCaptor.getValue().getStripeEventId());

        ArgumentCaptor<PaymentCompletedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(paymentEventPublisherPort).publishPaymentCompleted(eventCaptor.capture());
        assertEquals(payment.getId(), eventCaptor.getValue().paymentId());

        ArgumentCaptor<PaymentSucceededInternalEvent> internalCaptor =
                ArgumentCaptor.forClass(PaymentSucceededInternalEvent.class);
        verify(applicationEventPublisher).publishEvent(internalCaptor.capture());
        assertEquals(payment.getId(), internalCaptor.getValue().paymentId());
    }

    @Test
    void isNoOpWhenStripeEventAlreadyProcessed() {
        Payment payment = pendingPayment();
        when(paymentTransactionRepositoryPort.existsByStripeEventId(STRIPE_EVENT_ID)).thenReturn(true);
        when(paymentRepositoryPort.findByPaymentIntentId(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(payment));

        Payment result = service.execute(command());

        assertEquals(PaymentStatus.PENDING, result.getStatus());
        verify(paymentTransactionService, never()).confirmPayment(any(), any());
        verify(paymentEventPublisherPort, never()).publishPaymentCompleted(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void isNoOpWhenPaymentAlreadySucceeded() {
        Payment payment = succeededPayment();
        when(paymentTransactionRepositoryPort.existsByStripeEventId(STRIPE_EVENT_ID)).thenReturn(false);
        when(paymentRepositoryPort.findByPaymentIntentId(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(payment));

        Payment result = service.execute(command());

        assertEquals(PaymentStatus.SUCCEEDED, result.getStatus());
        verify(paymentTransactionService, never()).confirmPayment(any(), any());
        verify(paymentEventPublisherPort, never()).publishPaymentCompleted(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void throwsPaymentNotFoundExceptionWhenPaymentMissing() {
        when(paymentTransactionRepositoryPort.existsByStripeEventId(STRIPE_EVENT_ID)).thenReturn(false);
        when(paymentRepositoryPort.findByPaymentIntentId(PAYMENT_INTENT_ID)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> service.execute(command()));
    }

    private Payment pendingPayment() {
        return Payment.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "guest@example.com",
                PAYMENT_INTENT_ID,
                new BigDecimal("150.00"),
                "USD",
                PaymentStatus.PENDING,
                "cus_123",
                null,
                null,
                null,
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    private Payment succeededPayment() {
        return Payment.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "guest@example.com",
                PAYMENT_INTENT_ID,
                new BigDecimal("150.00"),
                "USD",
                PaymentStatus.SUCCEEDED,
                "cus_123",
                "card",
                "4242",
                PAID_AT,
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    private HandlePaymentSucceededCommand command() {
        return new HandlePaymentSucceededCommand(PAYMENT_INTENT_ID, STRIPE_EVENT_ID, "card", "4242", PAID_AT);
    }
}