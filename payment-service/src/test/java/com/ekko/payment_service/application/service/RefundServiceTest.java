package com.ekko.payment_service.application.service;

import com.ekko.payment_service.application.exception.PaymentNotFoundException;
import com.ekko.payment_service.domain.exception.RefundAmountExceededException;
import com.ekko.payment_service.domain.exception.RefundNotAllowedException;
import com.ekko.payment_service.domain.command.CreateRefundCommand;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.enums.PaymentStatus;
import com.ekko.payment_service.domain.model.Refund;
import com.ekko.payment_service.domain.enums.RefundReason;
import com.ekko.payment_service.domain.enums.RefundStatus;
import com.ekko.payment_service.domain.port.out.PaymentGatewayPort;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.domain.port.out.RefundRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final UUID REQUESTED_BY = UUID.randomUUID();
    private static final String PAYMENT_INTENT_ID = "pi_test_123";
    private static final String STRIPE_REFUND_ID = "re_test_123";
    private static final RefundReason REASON = RefundReason.DUPLICATE;

    @Mock
    private PaymentRepositoryPort paymentRepositoryPort;
    @Mock
    private RefundRepositoryPort refundRepositoryPort;
    @Mock
    private PaymentGatewayPort paymentGatewayPort;
    @Mock
    private RefundTransactionService refundTransactionService;

    private RefundService service;

    @BeforeEach
    void setUp() {
        service = new RefundService(
                paymentRepositoryPort,
                refundRepositoryPort,
                paymentGatewayPort,
                refundTransactionService);
    }

    @Test
    void happyPathRefundsSucceededPaymentWithinLimit() {
        Payment payment = succeededPayment(new BigDecimal("100.00"));
        when(paymentRepositoryPort.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(refundRepositoryPort.sumSucceededAmountByPaymentId(PAYMENT_ID)).thenReturn(BigDecimal.ZERO);
        when(paymentGatewayPort.createRefund(PAYMENT_INTENT_ID, new BigDecimal("40.00"), REASON))
                .thenReturn(STRIPE_REFUND_ID);
        when(refundTransactionService.commitRefund(any(Refund.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Refund result = service.execute(command(new BigDecimal("40.00")));

        assertEquals(RefundStatus.PENDING, result.getStatus());
        assertEquals(STRIPE_REFUND_ID, result.getStripeRefundId());
        assertEquals(PAYMENT_ID, result.getPaymentId());
        assertEquals(new BigDecimal("40.00"), result.getAmount());
        assertEquals(REASON, result.getReason());

        ArgumentCaptor<Refund> refundCaptor = ArgumentCaptor.forClass(Refund.class);
        verify(refundTransactionService).commitRefund(refundCaptor.capture());
        assertEquals(STRIPE_REFUND_ID, refundCaptor.getValue().getStripeRefundId());

        InOrder inOrder = inOrder(
                paymentRepositoryPort,
                refundRepositoryPort,
                paymentGatewayPort,
                refundTransactionService);
        inOrder.verify(paymentRepositoryPort).findById(PAYMENT_ID);
        inOrder.verify(refundRepositoryPort).sumSucceededAmountByPaymentId(PAYMENT_ID);
        inOrder.verify(paymentGatewayPort).createRefund(PAYMENT_INTENT_ID, new BigDecimal("40.00"), REASON);
        inOrder.verify(refundTransactionService).commitRefund(any(Refund.class));
    }

    @Test
    void throwsPaymentNotFoundExceptionWhenPaymentMissing() {
        when(paymentRepositoryPort.findById(PAYMENT_ID)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> service.execute(command(new BigDecimal("40.00"))));

        verify(paymentGatewayPort, never()).createRefund(any(), any(), any());
        verify(refundTransactionService, never()).commitRefund(any());
    }

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"PENDING", "CANCELLED"})
    void throwsRefundNotAllowedWhenPaymentNotSucceeded(PaymentStatus status) {
        Payment payment = paymentWithStatus(status, new BigDecimal("100.00"));
        when(paymentRepositoryPort.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));

        assertThrows(RefundNotAllowedException.class, () -> service.execute(command(new BigDecimal("40.00"))));

        verify(paymentGatewayPort, never()).createRefund(any(), any(), any());
        verify(refundTransactionService, never()).commitRefund(any());
    }

    @Test
    void throwsRefundAmountExceededWhenTotalWouldExceedPaymentAmount() {
        Payment payment = succeededPayment(new BigDecimal("100.00"));
        when(paymentRepositoryPort.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(refundRepositoryPort.sumSucceededAmountByPaymentId(PAYMENT_ID)).thenReturn(new BigDecimal("60.00"));

        assertThrows(RefundAmountExceededException.class, () -> service.execute(command(new BigDecimal("50.00"))));

        verify(paymentGatewayPort, never()).createRefund(any(), any(), any());
        verify(refundTransactionService, never()).commitRefund(any());
    }

    @Test
    void allowsTotalRefundWhenRefundedPlusRequestedEqualsPaymentAmountWithDifferentScales() {
        Payment payment = succeededPayment(new BigDecimal("100.00"));
        when(paymentRepositoryPort.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(refundRepositoryPort.sumSucceededAmountByPaymentId(PAYMENT_ID)).thenReturn(new BigDecimal("50.0"));
        when(paymentGatewayPort.createRefund(PAYMENT_INTENT_ID, new BigDecimal("50.00"), REASON))
                .thenReturn(STRIPE_REFUND_ID);
        when(refundTransactionService.commitRefund(any(Refund.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Refund result = service.execute(command(new BigDecimal("50.00")));

        assertEquals(RefundStatus.PENDING, result.getStatus());
        verify(paymentGatewayPort).createRefund(PAYMENT_INTENT_ID, new BigDecimal("50.00"), REASON);
        verify(refundTransactionService).commitRefund(any(Refund.class));
    }

    private Payment succeededPayment(BigDecimal amount) {
        return paymentWithStatus(PaymentStatus.SUCCEEDED, amount);
    }

    private Payment paymentWithStatus(PaymentStatus status, BigDecimal amount) {
        return Payment.restore(
                PAYMENT_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "guest@example.com",
                PAYMENT_INTENT_ID,
                amount,
                "USD",
                status,
                "cus_123",
                "card",
                "4242",
                LocalDateTime.now(),
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    private CreateRefundCommand command(BigDecimal amount) {
        return new CreateRefundCommand(PAYMENT_ID, amount, REASON, "test refund", REQUESTED_BY);
    }
}