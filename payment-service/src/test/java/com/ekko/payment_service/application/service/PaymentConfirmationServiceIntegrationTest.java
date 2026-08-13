package com.ekko.payment_service.application.service;

import com.ekko.payment_service.config.AbstractPostgresIntegrationTest;
import com.ekko.payment_service.domain.model.HandlePaymentSucceededCommand;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.model.PaymentCompletedEvent;
import com.ekko.payment_service.domain.model.PaymentStatus;
import com.ekko.payment_service.domain.port.in.HandlePaymentSucceededUseCase;
import com.ekko.payment_service.domain.port.out.PaymentEventPublisherPort;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.infrastructure.persistence.entity.PaymentTransactionEntity;
import com.ekko.payment_service.infrastructure.persistence.repository.PaymentJpaRepository;
import com.ekko.payment_service.infrastructure.persistence.repository.PaymentTransactionJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PaymentConfirmationServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String PAYMENT_INTENT_ID = "pi_test_123";
    private static final String STRIPE_EVENT_ID = "evt_test_123";
    private static final LocalDateTime PAID_AT = LocalDateTime.of(2026, 8, 11, 10, 30, 0);

    @Autowired
    private HandlePaymentSucceededUseCase handlePaymentSucceededUseCase;

    @Autowired
    private PaymentRepositoryPort paymentRepositoryPort;

    @Autowired
    private PaymentJpaRepository paymentJpaRepository;

    @Autowired
    private PaymentTransactionJpaRepository paymentTransactionJpaRepository;

    @MockitoBean
    private PaymentEventPublisherPort paymentEventPublisherPort;

    @AfterEach
    void cleanDatabase() {
        paymentTransactionJpaRepository.deleteAll();
        paymentJpaRepository.deleteAll();
    }

    @Test
    void confirmsPendingPaymentAndPersistsTransaction() {
        Payment persisted = savePendingPayment();

        Payment result = handlePaymentSucceededUseCase.execute(command());

        assertNotNull(result.getId());
        assertEquals(PaymentStatus.SUCCEEDED, result.getStatus());
        assertEquals(PAID_AT, result.getPaidAt());
        assertEquals("card", result.getPaymentMethodType());
        assertEquals("4242", result.getPaymentMethodLast4());

        assertEquals(PaymentStatus.SUCCEEDED,
                paymentJpaRepository.findByPaymentIntentId(PAYMENT_INTENT_ID).orElseThrow().getStatus());

        List<PaymentTransactionEntity> transactions = paymentTransactionJpaRepository.findAll();
        assertEquals(1, transactions.size());
        assertEquals(persisted.getId(), transactions.get(0).getPayment().getId());
        assertEquals(STRIPE_EVENT_ID, transactions.get(0).getStripeEventId());

        ArgumentCaptor<PaymentCompletedEvent> captor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(paymentEventPublisherPort, times(1)).publishPaymentCompleted(captor.capture());
        PaymentCompletedEvent event = captor.getValue();
        assertEquals(persisted.getId(), event.paymentId());
        assertEquals(persisted.getOrderId(), event.orderId());
        assertEquals(persisted.getCustomerId(), event.customerId());
        assertEquals(0, new BigDecimal("150.00").compareTo(event.amount()));
        assertEquals("USD", event.currency());
        assertEquals(PAID_AT, event.completedAt());
    }

    @Test
    void ignoresRepeatedWebhookForSameStripeEvent() {
        savePendingPayment();
        handlePaymentSucceededUseCase.execute(command());

        Payment second = handlePaymentSucceededUseCase.execute(command());

        assertEquals(PaymentStatus.SUCCEEDED, second.getStatus());
        assertEquals(1, paymentTransactionJpaRepository.findAll().size());
        verify(paymentEventPublisherPort, times(1))
                .publishPaymentCompleted(org.mockito.ArgumentMatchers.any(PaymentCompletedEvent.class));
    }

    @Test
    void ignoresPaymentAlreadySucceeded() {
        savePendingPayment();
        handlePaymentSucceededUseCase.execute(command());

        Payment alreadySucceeded = handlePaymentSucceededUseCase.execute(
                new HandlePaymentSucceededCommand(
                        PAYMENT_INTENT_ID,
                        "evt_test_other",
                        "card",
                        "4242",
                        PAID_AT));

        assertEquals(PaymentStatus.SUCCEEDED, alreadySucceeded.getStatus());
        assertEquals(1, paymentTransactionJpaRepository.findAll().size());
        assertTrue(paymentTransactionJpaRepository.existsByStripeEventId(STRIPE_EVENT_ID));
        verify(paymentEventPublisherPort, times(1))
                .publishPaymentCompleted(org.mockito.ArgumentMatchers.any(PaymentCompletedEvent.class));
    }

    private Payment savePendingPayment() {
        Payment payment = Payment.initiate(
                UUID.randomUUID(),
                null,
                "guest@example.com",
                new BigDecimal("150.00"),
                "USD");
        payment.attachPaymentIntent(PAYMENT_INTENT_ID, "cus_test_123");
        return paymentRepositoryPort.save(payment);
    }

    private HandlePaymentSucceededCommand command() {
        return new HandlePaymentSucceededCommand(
                PAYMENT_INTENT_ID,
                STRIPE_EVENT_ID,
                "card",
                "4242",
                PAID_AT);
    }
}