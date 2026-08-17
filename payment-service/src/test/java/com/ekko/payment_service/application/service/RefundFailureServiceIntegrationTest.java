package com.ekko.payment_service.application.service;

import com.ekko.payment_service.config.AbstractPostgresIntegrationTest;
import com.ekko.payment_service.domain.command.HandleRefundFailedCommand;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.event.PaymentRefundFailedEvent;
import com.ekko.payment_service.domain.enums.PaymentStatus;
import com.ekko.payment_service.domain.model.Refund;
import com.ekko.payment_service.domain.enums.RefundReason;
import com.ekko.payment_service.domain.enums.RefundStatus;
import com.ekko.payment_service.domain.enums.TransactionStatus;
import com.ekko.payment_service.domain.enums.TransactionType;
import com.ekko.payment_service.domain.port.in.HandleRefundFailedUseCase;
import com.ekko.payment_service.domain.port.out.PaymentEventPublisherPort;
import com.ekko.payment_service.domain.port.out.PaymentGatewayPort;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.domain.port.out.RefundRepositoryPort;
import com.ekko.payment_service.infrastructure.persistence.entity.PaymentTransactionEntity;
import com.ekko.payment_service.infrastructure.persistence.repository.PaymentJpaRepository;
import com.ekko.payment_service.infrastructure.persistence.repository.PaymentTransactionJpaRepository;
import com.ekko.payment_service.infrastructure.persistence.repository.RefundJpaRepository;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RefundFailureServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String STRIPE_REFUND_ID = "re_test_123";
    private static final String STRIPE_EVENT_ID = "evt_test_123";

    @Autowired
    private HandleRefundFailedUseCase handleRefundFailedUseCase;

    @Autowired
    private PaymentRepositoryPort paymentRepositoryPort;

    @Autowired
    private RefundRepositoryPort refundRepositoryPort;

    @Autowired
    private PaymentJpaRepository paymentJpaRepository;

    @Autowired
    private RefundJpaRepository refundJpaRepository;

    @Autowired
    private PaymentTransactionJpaRepository paymentTransactionJpaRepository;

    @MockitoBean
    private PaymentGatewayPort paymentGatewayPort;

    @MockitoBean
    private PaymentEventPublisherPort paymentEventPublisherPort;

    @AfterEach
    void cleanDatabase() {
        paymentTransactionJpaRepository.deleteAll();
        refundJpaRepository.deleteAll();
        paymentJpaRepository.deleteAll();
    }

    @Test
    void marksRefundFailedAndPersistsFailedTransactionWithoutChangingPayment() {
        Payment payment = saveSucceededPayment(new BigDecimal("100.00"));
        savePendingRefund(payment.getId(), new BigDecimal("40.00"));

        Refund result = handleRefundFailedUseCase.execute(command());

        assertEquals(RefundStatus.FAILED, result.getStatus());
        assertEquals(RefundStatus.FAILED,
                refundJpaRepository.findByStripeRefundId(STRIPE_REFUND_ID).orElseThrow().getStatus());
        assertEquals(PaymentStatus.SUCCEEDED, paymentJpaRepository.findById(payment.getId()).orElseThrow().getStatus());

        List<PaymentTransactionEntity> transactions = paymentTransactionJpaRepository.findAll();
        assertEquals(1, transactions.size());
        assertEquals(TransactionType.REFUND, transactions.get(0).getType());
        assertEquals(TransactionStatus.FAILED, transactions.get(0).getStatus());
        assertEquals(STRIPE_EVENT_ID, transactions.get(0).getStripeEventId());
        assertEquals(payment.getId(), transactions.get(0).getPayment().getId());

        ArgumentCaptor<PaymentRefundFailedEvent> captor = ArgumentCaptor.forClass(PaymentRefundFailedEvent.class);
        verify(paymentEventPublisherPort, times(1)).publishPaymentRefundFailed(captor.capture());
        assertEquals(payment.getId(), captor.getValue().paymentId());
    }

    private Payment saveSucceededPayment(BigDecimal amount) {
        Payment payment = Payment.initiate(
                UUID.randomUUID(),
                null,
                "guest@example.com",
                amount,
                "USD");
        payment.attachPaymentIntent("pi_test_123", "cus_test_123");
        payment.markSucceeded("card", "4242", LocalDateTime.now());
        return paymentRepositoryPort.save(payment);
    }

    private void savePendingRefund(UUID paymentId, BigDecimal amount) {
        Refund refund = Refund.initiate(paymentId, amount, RefundReason.DUPLICATE, UUID.randomUUID(), null);
        refund.attachStripeRefundId(STRIPE_REFUND_ID);
        refundRepositoryPort.save(refund);
    }

    private HandleRefundFailedCommand command() {
        return new HandleRefundFailedCommand(STRIPE_REFUND_ID, STRIPE_EVENT_ID);
    }
}