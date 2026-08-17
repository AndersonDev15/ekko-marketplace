package com.ekko.payment_service.application.service;

import com.ekko.payment_service.config.AbstractPostgresIntegrationTest;
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
import com.ekko.payment_service.infrastructure.persistence.entity.PaymentEntity;
import com.ekko.payment_service.infrastructure.persistence.repository.PaymentJpaRepository;
import com.ekko.payment_service.infrastructure.persistence.repository.PaymentTransactionJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PaymentCancellationServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String PAYMENT_INTENT_ID = "pi_test_123";
    private static final String STRIPE_EVENT_ID = "evt_test_123";

    @Autowired
    private HandlePaymentCancelledUseCase handlePaymentCancelledUseCase;

    @Autowired
    private PaymentRepositoryPort paymentRepositoryPort;

    @Autowired
    private PaymentTransactionRepositoryPort paymentTransactionRepositoryPort;

    @Autowired
    private PaymentJpaRepository paymentJpaRepository;

    @Autowired
    private PaymentTransactionJpaRepository paymentTransactionJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private PaymentEventPublisherPort paymentEventPublisherPort;

    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM payment_transactions");
        jdbcTemplate.execute("DELETE FROM payments");
    }

    @Test
    void cancelsPendingPaymentAndPersistsVoidTransaction() {
        savePendingPayment();

        Payment result = handlePaymentCancelledUseCase.execute(command());

        assertEquals(PaymentStatus.CANCELLED, result.getStatus());

        PaymentEntity persisted = paymentJpaRepository.findByPaymentIntentId(PAYMENT_INTENT_ID).orElseThrow();
        assertEquals(PaymentStatus.CANCELLED, persisted.getStatus());

        List<Map<String, Object>> transactions = jdbcTemplate.queryForList(
                "SELECT type, status, stripe_event_id FROM payment_transactions");
        assertEquals(1, transactions.size());
        assertEquals(TransactionType.VOID.name(), transactions.get(0).get("type"));
        assertEquals(TransactionStatus.SUCCEEDED.name(), transactions.get(0).get("status"));
        assertEquals(STRIPE_EVENT_ID, transactions.get(0).get("stripe_event_id"));

        ArgumentCaptor<PaymentCancelledEvent> captor = ArgumentCaptor.forClass(PaymentCancelledEvent.class);
        verify(paymentEventPublisherPort, times(1)).publishPaymentCancelled(captor.capture());
        assertEquals(persisted.getId(), captor.getValue().paymentId());
        assertEquals(persisted.getOrderId(), captor.getValue().orderId());
    }

    @Test
    void doesNotChangeAlreadySucceededPayment() {
        saveSucceededPayment();

        Payment result = handlePaymentCancelledUseCase.execute(command());

        assertEquals(PaymentStatus.SUCCEEDED, result.getStatus());

        PaymentEntity persisted = paymentJpaRepository.findByPaymentIntentId(PAYMENT_INTENT_ID).orElseThrow();
        assertEquals(PaymentStatus.SUCCEEDED, persisted.getStatus());
        assertEquals(0, paymentTransactionJpaRepository.count());
        verify(paymentEventPublisherPort, never()).publishPaymentCancelled(any());
    }

    @Test
    void ignoresDuplicateStripeEvent() {
        savePendingPayment();
        savePendingTransaction();

        Payment result = handlePaymentCancelledUseCase.execute(command());

        assertEquals(PaymentStatus.PENDING, result.getStatus());
        assertEquals(1, paymentTransactionJpaRepository.count());
        verify(paymentEventPublisherPort, never()).publishPaymentCancelled(any());
    }

    private void savePendingPayment() {
        Payment payment = Payment.initiate(
                UUID.randomUUID(),
                null,
                "guest@example.com",
                new BigDecimal("150.00"),
                "USD");
        payment.attachPaymentIntent(PAYMENT_INTENT_ID, "cus_test_123");
        paymentRepositoryPort.save(payment);
    }

    private void saveSucceededPayment() {
        Payment payment = Payment.initiate(
                UUID.randomUUID(),
                null,
                "guest@example.com",
                new BigDecimal("150.00"),
                "USD");
        payment.attachPaymentIntent(PAYMENT_INTENT_ID, "cus_test_123");
        payment.markSucceeded("card", "4242", LocalDateTime.now());
        paymentRepositoryPort.save(payment);
    }

    private void savePendingTransaction() {
        Payment persisted = paymentRepositoryPort.findByPaymentIntentId(PAYMENT_INTENT_ID).orElseThrow();
        PaymentTransaction transaction = PaymentTransaction.initiate(
                persisted.getId(),
                TransactionType.VOID,
                new BigDecimal("150.00"),
                "USD",
                TransactionStatus.PENDING,
                STRIPE_EVENT_ID,
                null);
        assertNotNull(paymentTransactionRepositoryPort.save(transaction).getId());
    }

    private HandlePaymentCancelledCommand command() {
        return new HandlePaymentCancelledCommand(PAYMENT_INTENT_ID, STRIPE_EVENT_ID);
    }
}