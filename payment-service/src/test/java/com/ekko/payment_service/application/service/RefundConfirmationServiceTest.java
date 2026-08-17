package com.ekko.payment_service.application.service;

import com.ekko.payment_service.application.exception.RefundNotFoundException;
import com.ekko.payment_service.domain.command.HandleRefundSucceededCommand;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.event.PaymentRefundedEvent;
import com.ekko.payment_service.domain.enums.PaymentStatus;
import com.ekko.payment_service.domain.model.Refund;
import com.ekko.payment_service.domain.enums.RefundReason;
import com.ekko.payment_service.domain.enums.RefundStatus;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundConfirmationServiceTest {

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

    private RefundConfirmationService service;

    @BeforeEach
    void setUp() {
        service = new RefundConfirmationService(
                refundRepositoryPort,
                paymentRepositoryPort,
                paymentTransactionRepositoryPort,
                paymentEventPublisherPort,
                refundTransactionService);
    }

    @Test
    void confirmsPartialRefundWithoutChangingPaymentStatus() {
        Payment payment = succeededPayment(new BigDecimal("100.00"));
        Refund refund = pendingRefund(new BigDecimal("40.00"));
        when(paymentTransactionRepositoryPort.existsByStripeEventId(STRIPE_EVENT_ID)).thenReturn(false);
        when(refundRepositoryPort.findByStripeRefundId(STRIPE_REFUND_ID)).thenReturn(Optional.of(refund));
        when(paymentRepositoryPort.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(refundRepositoryPort.sumSucceededAmountByPaymentId(PAYMENT_ID)).thenReturn(BigDecimal.ZERO);
        when(refundTransactionService.confirmRefund(any(Refund.class), any(Payment.class), any()))
                .thenAnswer(inv -> inv.getArgument(0));

        Refund result = service.execute(command());

        assertEquals(RefundStatus.SUCCEEDED, result.getStatus());

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(refundTransactionService).confirmRefund(any(Refund.class), paymentCaptor.capture(), any());
        assertEquals(PaymentStatus.SUCCEEDED, paymentCaptor.getValue().getStatus());

        ArgumentCaptor<PaymentRefundedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentRefundedEvent.class);
        verify(paymentEventPublisherPort).publishPaymentRefunded(eventCaptor.capture());
        assertEquals(PAYMENT_ID, eventCaptor.getValue().paymentId());
        assertTrue(eventCaptor.getValue().refundId() != null);
        assertEquals(0, new BigDecimal("40.00").compareTo(eventCaptor.getValue().amount()));
        assertFalse(eventCaptor.getValue().isTotal());
    }

    @Test
    void confirmsTotalRefundMarkingPaymentRefunded() {
        Payment payment = succeededPayment(new BigDecimal("100.00"));
        Refund refund = pendingRefund(new BigDecimal("100.00"));
        when(paymentTransactionRepositoryPort.existsByStripeEventId(STRIPE_EVENT_ID)).thenReturn(false);
        when(refundRepositoryPort.findByStripeRefundId(STRIPE_REFUND_ID)).thenReturn(Optional.of(refund));
        when(paymentRepositoryPort.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(refundRepositoryPort.sumSucceededAmountByPaymentId(PAYMENT_ID)).thenReturn(BigDecimal.ZERO);
        when(refundTransactionService.confirmRefund(any(Refund.class), any(Payment.class), any()))
                .thenAnswer(inv -> inv.getArgument(0));

        Refund result = service.execute(command());

        assertEquals(RefundStatus.SUCCEEDED, result.getStatus());

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(refundTransactionService).confirmRefund(any(Refund.class), paymentCaptor.capture(), any());
        assertEquals(PaymentStatus.REFUNDED, paymentCaptor.getValue().getStatus());

        ArgumentCaptor<PaymentRefundedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentRefundedEvent.class);
        verify(paymentEventPublisherPort).publishPaymentRefunded(eventCaptor.capture());
        assertEquals(PAYMENT_ID, eventCaptor.getValue().paymentId());
        assertEquals(0, new BigDecimal("100.00").compareTo(eventCaptor.getValue().amount()));
        assertTrue(eventCaptor.getValue().isTotal());
    }

    @Test
    void isNoOpWhenStripeEventAlreadyProcessed() {
        Refund refund = pendingRefund(new BigDecimal("40.00"));
        when(paymentTransactionRepositoryPort.existsByStripeEventId(STRIPE_EVENT_ID)).thenReturn(true);
        when(refundRepositoryPort.findByStripeRefundId(STRIPE_REFUND_ID)).thenReturn(Optional.of(refund));

        Refund result = service.execute(command());

        assertEquals(RefundStatus.PENDING, result.getStatus());
        verify(refundTransactionService, never()).confirmRefund(any(), any(), any());
        verify(paymentEventPublisherPort, never()).publishPaymentRefunded(any());
    }

    @Test
    void isNoOpWhenRefundAlreadySucceeded() {
        Refund refund = refundWithStatus(RefundStatus.SUCCEEDED, new BigDecimal("40.00"));
        when(paymentTransactionRepositoryPort.existsByStripeEventId(STRIPE_EVENT_ID)).thenReturn(false);
        when(refundRepositoryPort.findByStripeRefundId(STRIPE_REFUND_ID)).thenReturn(Optional.of(refund));

        Refund result = service.execute(command());

        assertEquals(RefundStatus.SUCCEEDED, result.getStatus());
        verify(refundTransactionService, never()).confirmRefund(any(), any(), any());
        verify(paymentEventPublisherPort, never()).publishPaymentRefunded(any());
    }

    @Test
    void isNoOpWhenRefundAlreadyFailed() {
        Refund refund = refundWithStatus(RefundStatus.FAILED, new BigDecimal("40.00"));
        when(paymentTransactionRepositoryPort.existsByStripeEventId(STRIPE_EVENT_ID)).thenReturn(false);
        when(refundRepositoryPort.findByStripeRefundId(STRIPE_REFUND_ID)).thenReturn(Optional.of(refund));

        Refund result = service.execute(command());

        assertEquals(RefundStatus.FAILED, result.getStatus());
        verify(refundTransactionService, never()).confirmRefund(any(), any(), any());
        verify(paymentEventPublisherPort, never()).publishPaymentRefunded(any());
    }

    @Test
    void throwsRefundNotFoundExceptionWhenRefundMissing() {
        when(paymentTransactionRepositoryPort.existsByStripeEventId(STRIPE_EVENT_ID)).thenReturn(false);
        when(refundRepositoryPort.findByStripeRefundId(STRIPE_REFUND_ID)).thenReturn(Optional.empty());

        assertThrows(RefundNotFoundException.class, () -> service.execute(command()));

        verify(refundTransactionService, never()).confirmRefund(any(), any(), any());
        verify(paymentEventPublisherPort, never()).publishPaymentRefunded(any());
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

    private HandleRefundSucceededCommand command() {
        return new HandleRefundSucceededCommand(STRIPE_REFUND_ID, STRIPE_EVENT_ID);
    }
}