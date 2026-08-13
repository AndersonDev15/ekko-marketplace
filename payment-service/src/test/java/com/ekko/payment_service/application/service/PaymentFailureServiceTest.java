package com.ekko.payment_service.application.service;

import com.ekko.payment_service.domain.exception.PaymentNotFoundException;
import com.ekko.payment_service.domain.model.HandlePaymentFailedCommand;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.model.PaymentFailedEvent;
import com.ekko.payment_service.domain.model.PaymentFailureReason;
import com.ekko.payment_service.domain.model.PaymentStatus;
import com.ekko.payment_service.domain.model.PaymentTransaction;
import com.ekko.payment_service.domain.model.TransactionStatus;
import com.ekko.payment_service.domain.model.TransactionType;
import com.ekko.payment_service.domain.port.out.PaymentEventPublisherPort;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.domain.port.out.PaymentTransactionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class PaymentFailureServiceTest {

    private static final String PAYMENT_INTENT_ID = "pi_test_123";
    private static final String STRIPE_EVENT_ID = "evt_test_123";
    private static final PaymentFailureReason REASON = PaymentFailureReason.DECLINED;

    @Mock
    private PaymentRepositoryPort paymentRepositoryPort;
    @Mock
    private PaymentTransactionRepositoryPort paymentTransactionRepositoryPort;
    @Mock
    private PaymentEventPublisherPort paymentEventPublisherPort;
    @Mock
    private PaymentTransactionService paymentTransactionService;

    private PaymentFailureService service;

    @BeforeEach
    void setUp() {
        service = new PaymentFailureService(
                paymentRepositoryPort,
                paymentTransactionRepositoryPort,
                paymentEventPublisherPort,
                paymentTransactionService);
    }

    @Test
    void marksPendingPaymentFailedAndPersistsTransactionWithErrorMessage() {
        Payment payment = pendingPayment();
        when(paymentTransactionRepositoryPort.existsByStripeEventId(STRIPE_EVENT_ID)).thenReturn(false);
        when(paymentRepositoryPort.findByPaymentIntentId(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(payment));
        when(paymentTransactionService.failPayment(any(Payment.class), any(PaymentTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Payment result = service.execute(command("card_declined"));

        assertEquals(PaymentStatus.FAILED, result.getStatus());

        ArgumentCaptor<PaymentTransaction> transactionCaptor =
                ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionService).failPayment(eq(payment), transactionCaptor.capture());
        assertEquals(TransactionType.CHARGE, transactionCaptor.getValue().getType());
        assertEquals(TransactionStatus.FAILED, transactionCaptor.getValue().getStatus());
        assertEquals(payment.getId(), transactionCaptor.getValue().getPaymentId());
        assertEquals(STRIPE_EVENT_ID, transactionCaptor.getValue().getStripeEventId());
        assertEquals("card_declined", transactionCaptor.getValue().getErrorMessage());

        ArgumentCaptor<PaymentFailedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentFailedEvent.class);
        verify(paymentEventPublisherPort).publishPaymentFailed(eventCaptor.capture());
        assertEquals(payment.getId(), eventCaptor.getValue().paymentId());
        assertEquals(REASON, eventCaptor.getValue().reason());
    }

    @Test
    void isNoOpWhenStripeEventAlreadyProcessed() {
        Payment payment = pendingPayment();
        when(paymentTransactionRepositoryPort.existsByStripeEventId(STRIPE_EVENT_ID)).thenReturn(true);
        when(paymentRepositoryPort.findByPaymentIntentId(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(payment));

        Payment result = service.execute(command("card_declined"));

        assertEquals(PaymentStatus.PENDING, result.getStatus());
        verify(paymentTransactionService, never()).failPayment(any(), any());
        verify(paymentEventPublisherPort, never()).publishPaymentFailed(any());
    }

    @Test
    void isNoOpWhenPaymentAlreadySucceeded() {
        Payment payment = succeededPayment();
        when(paymentTransactionRepositoryPort.existsByStripeEventId(STRIPE_EVENT_ID)).thenReturn(false);
        when(paymentRepositoryPort.findByPaymentIntentId(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(payment));

        Payment result = service.execute(command("card_declined"));

        assertEquals(PaymentStatus.SUCCEEDED, result.getStatus());
        verify(paymentTransactionService, never()).failPayment(any(), any());
        verify(paymentEventPublisherPort, never()).publishPaymentFailed(any());
    }

    @Test
    void isNoOpWhenPaymentAlreadyFailed() {
        Payment payment = failedPayment();
        when(paymentTransactionRepositoryPort.existsByStripeEventId(STRIPE_EVENT_ID)).thenReturn(false);
        when(paymentRepositoryPort.findByPaymentIntentId(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(payment));

        Payment result = service.execute(command("card_declined"));

        assertEquals(PaymentStatus.FAILED, result.getStatus());
        verify(paymentTransactionService, never()).failPayment(any(), any());
        verify(paymentEventPublisherPort, never()).publishPaymentFailed(any());
    }

    @Test
    void throwsPaymentNotFoundExceptionWhenPaymentMissing() {
        when(paymentTransactionRepositoryPort.existsByStripeEventId(STRIPE_EVENT_ID)).thenReturn(false);
        when(paymentRepositoryPort.findByPaymentIntentId(PAYMENT_INTENT_ID)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> service.execute(command("card_declined")));
    }

    private Payment pendingPayment() {
        return paymentWithStatus(PaymentStatus.PENDING);
    }

    private Payment succeededPayment() {
        return paymentWithStatus(PaymentStatus.SUCCEEDED);
    }

    private Payment failedPayment() {
        return paymentWithStatus(PaymentStatus.FAILED);
    }

    private Payment paymentWithStatus(PaymentStatus status) {
        return Payment.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "guest@example.com",
                PAYMENT_INTENT_ID,
                new BigDecimal("150.00"),
                "USD",
                status,
                "cus_123",
                null,
                null,
                null,
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    private HandlePaymentFailedCommand command(String errorMessage) {
        return new HandlePaymentFailedCommand(PAYMENT_INTENT_ID, STRIPE_EVENT_ID, REASON, errorMessage);
    }
}