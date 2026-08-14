package com.ekko.payment_service.application.service;

import com.ekko.payment_service.domain.exception.RefundNotFoundException;
import com.ekko.payment_service.domain.model.HandleRefundFailedCommand;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.model.PaymentRefundFailedEvent;
import com.ekko.payment_service.domain.model.PaymentStatus;
import com.ekko.payment_service.domain.model.PaymentTransaction;
import com.ekko.payment_service.domain.model.Refund;
import com.ekko.payment_service.domain.model.RefundReason;
import com.ekko.payment_service.domain.model.RefundStatus;
import com.ekko.payment_service.domain.model.TransactionStatus;
import com.ekko.payment_service.domain.model.TransactionType;
import com.ekko.payment_service.domain.port.out.PaymentEventPublisherPort;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.domain.port.out.PaymentTransactionRepositoryPort;
import com.ekko.payment_service.domain.port.out.RefundRepositoryPort;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundFailureServiceTest {

    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final String STRIPE_REFUND_ID = "re_test_123";
    private static final String STRIPE_EVENT_ID = "evt_test_123";

    @Mock
    private RefundRepositoryPort refundRepositoryPort;
    @Mock
    private PaymentRepositoryPort paymentRepositoryPort;
    @Mock
    private PaymentTransactionRepositoryPort paymentTransactionRepositoryPort;
    @Mock
    private PaymentEventPublisherPort paymentEventPublisherPort;
    @Mock
    private RefundTransactionService refundTransactionService;

    private RefundFailureService service;

    @BeforeEach
    void setUp() {
        service = new RefundFailureService(
                refundRepositoryPort,
                paymentRepositoryPort,
                paymentTransactionRepositoryPort,
                paymentEventPublisherPort,
                refundTransactionService);
    }

    @Test
    void marksRefundFailedAndPersistsRefundTransactionWithoutTouchingPayment() {
        Payment payment = succeededPayment(new BigDecimal("100.00"));
        Refund refund = pendingRefund(new BigDecimal("40.00"));
        when(paymentTransactionRepositoryPort.existsByStripeEventId(STRIPE_EVENT_ID)).thenReturn(false);
        when(refundRepositoryPort.findByStripeRefundId(STRIPE_REFUND_ID)).thenReturn(Optional.of(refund));
        when(paymentRepositoryPort.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(refundTransactionService.failRefund(any(Refund.class), any(PaymentTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Refund result = service.execute(command());

        assertEquals(RefundStatus.FAILED, result.getStatus());

        ArgumentCaptor<PaymentTransaction> transactionCaptor =
                ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(refundTransactionService).failRefund(any(Refund.class), transactionCaptor.capture());
        assertEquals(TransactionType.REFUND, transactionCaptor.getValue().getType());
        assertEquals(TransactionStatus.FAILED, transactionCaptor.getValue().getStatus());
        assertEquals(PAYMENT_ID, transactionCaptor.getValue().getPaymentId());
        assertEquals(STRIPE_EVENT_ID, transactionCaptor.getValue().getStripeEventId());

        verify(paymentRepositoryPort, never()).save(any(Payment.class));

        ArgumentCaptor<PaymentRefundFailedEvent> eventCaptor =
                ArgumentCaptor.forClass(PaymentRefundFailedEvent.class);
        verify(paymentEventPublisherPort).publishPaymentRefundFailed(eventCaptor.capture());
        assertEquals(PAYMENT_ID, eventCaptor.getValue().paymentId());
        assertTrue(eventCaptor.getValue().refundId() != null);
    }

    @Test
    void isNoOpWhenStripeEventAlreadyProcessed() {
        Refund refund = pendingRefund(new BigDecimal("40.00"));
        when(paymentTransactionRepositoryPort.existsByStripeEventId(STRIPE_EVENT_ID)).thenReturn(true);
        when(refundRepositoryPort.findByStripeRefundId(STRIPE_REFUND_ID)).thenReturn(Optional.of(refund));

        Refund result = service.execute(command());

        assertEquals(RefundStatus.PENDING, result.getStatus());
        verify(refundTransactionService, never()).failRefund(any(), any());
        verify(paymentEventPublisherPort, never()).publishPaymentRefundFailed(any());
    }

    @Test
    void isNoOpWhenRefundAlreadySucceeded() {
        Refund refund = refundWithStatus(RefundStatus.SUCCEEDED, new BigDecimal("40.00"));
        when(paymentTransactionRepositoryPort.existsByStripeEventId(STRIPE_EVENT_ID)).thenReturn(false);
        when(refundRepositoryPort.findByStripeRefundId(STRIPE_REFUND_ID)).thenReturn(Optional.of(refund));

        Refund result = service.execute(command());

        assertEquals(RefundStatus.SUCCEEDED, result.getStatus());
        verify(refundTransactionService, never()).failRefund(any(), any());
        verify(paymentEventPublisherPort, never()).publishPaymentRefundFailed(any());
    }

    @Test
    void isNoOpWhenRefundAlreadyFailed() {
        Refund refund = refundWithStatus(RefundStatus.FAILED, new BigDecimal("40.00"));
        when(paymentTransactionRepositoryPort.existsByStripeEventId(STRIPE_EVENT_ID)).thenReturn(false);
        when(refundRepositoryPort.findByStripeRefundId(STRIPE_REFUND_ID)).thenReturn(Optional.of(refund));

        Refund result = service.execute(command());

        assertEquals(RefundStatus.FAILED, result.getStatus());
        verify(refundTransactionService, never()).failRefund(any(), any());
        verify(paymentEventPublisherPort, never()).publishPaymentRefundFailed(any());
    }

    @Test
    void throwsRefundNotFoundExceptionWhenRefundMissing() {
        when(paymentTransactionRepositoryPort.existsByStripeEventId(STRIPE_EVENT_ID)).thenReturn(false);
        when(refundRepositoryPort.findByStripeRefundId(STRIPE_REFUND_ID)).thenReturn(Optional.empty());

        assertThrows(RefundNotFoundException.class, () -> service.execute(command()));

        verify(refundTransactionService, never()).failRefund(any(), any());
        verify(paymentEventPublisherPort, never()).publishPaymentRefundFailed(any());
    }

    private Payment succeededPayment(BigDecimal amount) {
        return Payment.restore(
                PAYMENT_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "guest@example.com",
                "pi_test_123",
                amount,
                "USD",
                PaymentStatus.SUCCEEDED,
                "cus_123",
                "card",
                "4242",
                LocalDateTime.now(),
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    private Refund pendingRefund(BigDecimal amount) {
        return refundWithStatus(RefundStatus.PENDING, amount);
    }

    private Refund refundWithStatus(RefundStatus status, BigDecimal amount) {
        return Refund.restore(
                UUID.randomUUID(),
                PAYMENT_ID,
                STRIPE_REFUND_ID,
                amount,
                RefundReason.DUPLICATE,
                status,
                UUID.randomUUID(),
                "test refund",
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    private HandleRefundFailedCommand command() {
        return new HandleRefundFailedCommand(STRIPE_REFUND_ID, STRIPE_EVENT_ID);
    }
}