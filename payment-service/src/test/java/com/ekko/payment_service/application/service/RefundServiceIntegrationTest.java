package com.ekko.payment_service.application.service;

import com.ekko.payment_service.config.AbstractPostgresIntegrationTest;
import com.ekko.payment_service.domain.exception.RefundAmountExceededException;
import com.ekko.payment_service.domain.command.CreateRefundCommand;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.model.Refund;
import com.ekko.payment_service.domain.enums.RefundReason;
import com.ekko.payment_service.domain.enums.RefundStatus;
import com.ekko.payment_service.domain.port.out.PaymentEventPublisherPort;
import com.ekko.payment_service.domain.port.out.PaymentGatewayPort;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.domain.port.out.RefundRepositoryPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefundServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String PAYMENT_INTENT_ID = "pi_test_123";
    private static final String STRIPE_REFUND_ID = "re_test_123";
    private static final UUID REQUESTED_BY = UUID.randomUUID();

    @Autowired
    private CreateRefundUseCase createRefundUseCase;

    @Autowired
    private PaymentRepositoryPort paymentRepositoryPort;

    @Autowired
    private RefundRepositoryPort refundRepositoryPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private PaymentGatewayPort paymentGatewayPort;

    @MockitoBean
    private PaymentEventPublisherPort paymentEventPublisherPort;

    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM refunds");
        jdbcTemplate.execute("DELETE FROM payment_transactions");
        jdbcTemplate.execute("DELETE FROM payments");
    }

    @Test
    void happyPathPersistsPendingRefundWithStripeRefundId() {
        Payment payment = saveSucceededPayment(new BigDecimal("100.00"));
        when(paymentGatewayPort.createRefund(eq(PAYMENT_INTENT_ID), any(BigDecimal.class), any(RefundReason.class)))
                .thenReturn(STRIPE_REFUND_ID);

        Refund result = createRefundUseCase.execute(command(payment.getId(), new BigDecimal("40.00")));

        assertEquals(RefundStatus.PENDING, result.getStatus());
        assertEquals(STRIPE_REFUND_ID, result.getStripeRefundId());

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT status, stripe_refund_id, amount FROM refunds WHERE payment_id = ?",
                payment.getId());
        assertEquals(1, rows.size());
        assertEquals(RefundStatus.PENDING.name(), rows.get(0).get("status"));
        assertEquals(STRIPE_REFUND_ID, rows.get(0).get("stripe_refund_id"));
        assertEquals(0, new BigDecimal("40.00").compareTo((BigDecimal) rows.get(0).get("amount")));

        verify(paymentGatewayPort).createRefund(PAYMENT_INTENT_ID, new BigDecimal("40.00"), RefundReason.DUPLICATE);
    }

    @Test
    void throwsRefundAmountExceededWithoutPersistingNewRefund() {
        Payment payment = saveSucceededPayment(new BigDecimal("100.00"));
        saveSucceededRefund(payment.getId(), new BigDecimal("60.00"));

        assertThrows(RefundAmountExceededException.class,
                () -> createRefundUseCase.execute(command(payment.getId(), new BigDecimal("50.00"))));

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT id FROM refunds");
        assertEquals(1, rows.size());
        verify(paymentGatewayPort, never()).createRefund(any(), any(), any());
    }

    @Test
    void sumSucceededAmountByPaymentIdReturnsZeroWhenNoRefundsExist() {
        Payment payment = saveSucceededPayment(new BigDecimal("100.00"));

        BigDecimal sum = refundRepositoryPort.sumSucceededAmountByPaymentId(payment.getId());

        assertEquals(BigDecimal.ZERO, sum);
        assertEquals(0, BigDecimal.ZERO.compareTo(sum));
    }

    private Payment saveSucceededPayment(BigDecimal amount) {
        Payment payment = Payment.initiate(
                UUID.randomUUID(),
                null,
                "guest@example.com",
                amount,
                "USD");
        payment.attachPaymentIntent(PAYMENT_INTENT_ID, "cus_test_123");
        payment.markSucceeded("card", "4242", LocalDateTime.now());
        return paymentRepositoryPort.save(payment);
    }

    private void saveSucceededRefund(UUID paymentId, BigDecimal amount) {
        Refund refund = Refund.initiate(paymentId, amount, RefundReason.DUPLICATE, REQUESTED_BY, null);
        refund.attachStripeRefundId("re_prev_" + amount);
        refund.markSucceeded();
        refundRepositoryPort.save(refund);
    }

    private CreateRefundCommand command(UUID paymentId, BigDecimal amount) {
        return new CreateRefundCommand(paymentId, amount, RefundReason.DUPLICATE, "integration refund", REQUESTED_BY);
    }
}