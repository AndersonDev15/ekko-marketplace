package com.ekko.payment_service.application.service;

import com.ekko.payment_service.domain.exception.PaymentNotFoundException;
import com.ekko.payment_service.domain.model.HandlePaymentCancelledCommand;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.model.PaymentCancelledEvent;
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
class PaymentCancellationServiceTest {

    private static final String PAYMENT_INTENT_ID = "pi_test_123";
    private static final String STRIPE_EVENT_ID = "evt_test_123";

    @Mock
    private PaymentRepositoryPort paymentRepositoryPort;
    @Mock
    private PaymentTransactionRepositoryPort paymentTransactionRepositoryPort;
    @Mock
    private PaymentEventPublisherPort paymentEventPublisherPort;
    @Mock
    private PaymentTransactionService paymentTransactionService;

    private PaymentCancellationService service;

    @BeforeEach
    void setUp() {
        service = new PaymentCancellationService(
                paymentRepositoryPort,
                paymentTransactionRepositoryPort,
                paymentEventPublisherPort,
                paymentTransactionService);
    }

    @Test
    void cancelsPendingPaymentPersistsVoidTransactionAndPublishesEvent() {
        Payment payment = paymentWithStatus(PaymentStatus.PENDING);
        when(paymentTransactionRepositoryPort.existsByStripeEventId(STRIPE_EVENT_ID)).thenReturn(false);
        when(paymentRepositoryPort.findByPaymentIntentId(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(payment));
        when(paymentTransactionService.cancelPayment(any(Payment.class), any(PaymentTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Payment result = service.execute(command());

        assertEquals(PaymentStatus.CANCELLED, result.getStatus());

        ArgumentCaptor<PaymentTransaction> transactionCaptor =
                ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionService).cancelPayment(eq(payment), transactionCaptor.capture());
        assertEquals(TransactionType.VOID, transactionCaptor.getValue().getType());
        assertEquals(TransactionStatus.SUCCEEDED, transactionCaptor.getValue().getStatus());
        assertEquals(payment.getId(), transactionCaptor.getValue().getPaymentId());
        assertEquals(payment.getAmount(), transactionCaptor.getValue().getAmount());
        assertEquals(payment.getCurrency(), transactionCaptor.getValue().getCurrency());
        assertEquals(STRIPE_EVENT_ID, transactionCaptor.getValue().getStripeEventId());

        ArgumentCaptor<PaymentCancelledEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCancelledEvent.class);
        verify(paymentEventPublisherPort).publishPaymentCancelled(eventCaptor.capture());
        assertEquals(payment.getId(), eventCaptor.getValue().paymentId());
        assertEquals(payment.getOrderId(), eventCaptor.getValue().orderId());
        assertEquals(payment.getCustomerId(), eventCaptor.getValue().customerId());
    }

    @Test
    void isNoOpWhenPaymentAlreadySucceeded() {
        Payment payment = paymentWithStatus(PaymentStatus.SUCCEEDED);
        when(paymentTransactionRepositoryPort.existsByStripeEventId(STRIPE_EVENT_ID)).thenReturn(false);
        when(paymentRepositoryPort.findByPaymentIntentId(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(payment));

        Payment result = service.execute(command());

        assertEquals(PaymentStatus.SUCCEEDED, result.getStatus());
        verify(paymentTransactionService, never()).cancelPayment(any(), any());
        verify(paymentEventPublisherPort, never()).publishPaymentCancelled(any());
    }

    @Test
    void isNoOpWhenPaymentAlreadyCancelled() {
        Payment payment = paymentWithStatus(PaymentStatus.CANCELLED);
        when(paymentTransactionRepositoryPort.existsByStripeEventId(STRIPE_EVENT_ID)).thenReturn(false);
        when(paymentRepositoryPort.findByPaymentIntentId(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(payment));

        Payment result = service.execute(command());

        assertEquals(PaymentStatus.CANCELLED, result.getStatus());
        verify(paymentTransactionService, never()).cancelPayment(any(), any());
        verify(paymentEventPublisherPort, never()).publishPaymentCancelled(any());
    }

    @Test
    void isNoOpWhenStripeEventAlreadyProcessed() {
        Payment payment = paymentWithStatus(PaymentStatus.PENDING);
        when(paymentTransactionRepositoryPort.existsByStripeEventId(STRIPE_EVENT_ID)).thenReturn(true);
        when(paymentRepositoryPort.findByPaymentIntentId(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(payment));

        Payment result = service.execute(command());

        assertEquals(PaymentStatus.PENDING, result.getStatus());
        verify(paymentTransactionService, never()).cancelPayment(any(), any());
        verify(paymentEventPublisherPort, never()).publishPaymentCancelled(any());
    }

    @Test
    void throwsPaymentNotFoundExceptionWhenPaymentMissing() {
        when(paymentTransactionRepositoryPort.existsByStripeEventId(STRIPE_EVENT_ID)).thenReturn(false);
        when(paymentRepositoryPort.findByPaymentIntentId(PAYMENT_INTENT_ID)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> service.execute(command()));
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

    private HandlePaymentCancelledCommand command() {
        return new HandlePaymentCancelledCommand(PAYMENT_INTENT_ID, STRIPE_EVENT_ID);
    }
}