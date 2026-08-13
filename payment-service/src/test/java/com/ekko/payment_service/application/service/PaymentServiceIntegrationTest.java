package com.ekko.payment_service.application.service;

import com.ekko.payment_service.config.AbstractPostgresIntegrationTest;
import com.ekko.payment_service.domain.exception.PaymentAlreadySucceededException;
import com.ekko.payment_service.domain.exception.VendorAccountNotFoundException;
import com.ekko.payment_service.domain.model.InitiatePaymentCommand;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.model.PaymentInitiatedEvent;
import com.ekko.payment_service.domain.model.PaymentStatus;
import com.ekko.payment_service.domain.port.in.InitiatePaymentUseCase;
import com.ekko.payment_service.domain.port.out.PaymentEventPublisherPort;
import com.ekko.payment_service.domain.port.out.PaymentGatewayPort;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.infrastructure.persistence.entity.PaymentEntity;
import com.ekko.payment_service.infrastructure.persistence.entity.VendorAccountStatus;
import com.ekko.payment_service.infrastructure.persistence.entity.VendorStripeAccountEntity;
import com.ekko.payment_service.infrastructure.persistence.repository.PaymentJpaRepository;
import com.ekko.payment_service.infrastructure.persistence.repository.VendorStripeAccountJpaRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID VENDOR_ONE = UUID.randomUUID();
    private static final UUID VENDOR_TWO = UUID.randomUUID();
    private static final String PAYMENT_INTENT_ID = "pi_test_123";
    private static final String STRIPE_CUSTOMER_ID = "cus_test_123";

    @Autowired
    private InitiatePaymentUseCase initiatePaymentUseCase;

    @Autowired
    private PaymentRepositoryPort paymentRepositoryPort;

    @Autowired
    private PaymentJpaRepository paymentJpaRepository;

    @Autowired
    private VendorStripeAccountJpaRepository vendorStripeAccountJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private PaymentGatewayPort paymentGatewayPort;

    @MockitoBean
    private PaymentEventPublisherPort paymentEventPublisherPort;

    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM payment_vendor_allocations");
        jdbcTemplate.execute("DELETE FROM payment_transactions");
        jdbcTemplate.execute("DELETE FROM payment_transfers");
        jdbcTemplate.execute("DELETE FROM payments");
        jdbcTemplate.execute("DELETE FROM vendor_stripe_accounts");
    }

    @Test
    void happyPathInitiatesPaymentAndPersistsPendingPaymentWithAllocations() {
        saveVendorAccount(VENDOR_ONE, "acct_1");
        saveVendorAccount(VENDOR_TWO, "acct_2");
        when(paymentGatewayPort.resolveOrCreateCustomer(eq(null), eq("guest@example.com")))
                .thenReturn(STRIPE_CUSTOMER_ID);
        when(paymentGatewayPort.createPaymentIntent(any(BigDecimal.class), eq("USD"), eq(STRIPE_CUSTOMER_ID),
                eq(ORDER_ID.toString())))
                .thenReturn(PAYMENT_INTENT_ID);

        Payment result = initiatePaymentUseCase.execute(command());

        assertNotNull(result.getId());
        assertEquals(ORDER_ID, result.getOrderId());
        assertEquals(new BigDecimal("150.00"), result.getAmount());
        assertEquals(PAYMENT_INTENT_ID, result.getPaymentIntentId());
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        assertEquals(2, result.getAllocations().size());

        PaymentEntity persisted = paymentJpaRepository.findByPaymentIntentId(PAYMENT_INTENT_ID).orElseThrow();
        assertEquals(PaymentStatus.PENDING, persisted.getStatus());
        assertEquals(PAYMENT_INTENT_ID, persisted.getPaymentIntentId());

        List<Map<String, Object>> allocations = jdbcTemplate.queryForList(
                "SELECT vendor_id, gross_amount, application_fee_amount, net_amount "
                        + "FROM payment_vendor_allocations WHERE payment_id = ?",
                persisted.getId());
        assertEquals(2, allocations.size());

        Map<String, Object> allocationOne = allocations.stream()
                .filter(a -> a.get("vendor_id").equals(VENDOR_ONE))
                .findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("100.00").compareTo((BigDecimal) allocationOne.get("gross_amount")));
        assertEquals(0, new BigDecimal("10.00").compareTo((BigDecimal) allocationOne.get("application_fee_amount")));
        assertEquals(0, new BigDecimal("90.00").compareTo((BigDecimal) allocationOne.get("net_amount")));

        Map<String, Object> allocationTwo = allocations.stream()
                .filter(a -> a.get("vendor_id").equals(VENDOR_TWO))
                .findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("50.00").compareTo((BigDecimal) allocationTwo.get("gross_amount")));
        assertEquals(0, new BigDecimal("5.00").compareTo((BigDecimal) allocationTwo.get("application_fee_amount")));
        assertEquals(0, new BigDecimal("45.00").compareTo((BigDecimal) allocationTwo.get("net_amount")));

        ArgumentCaptor<PaymentInitiatedEvent> captor = ArgumentCaptor.forClass(PaymentInitiatedEvent.class);
        verify(paymentEventPublisherPort, times(1)).publishPaymentInitiated(captor.capture());
        assertEquals(persisted.getId(), captor.getValue().paymentId());
        assertEquals(ORDER_ID, captor.getValue().orderId());
        assertEquals(PAYMENT_INTENT_ID, captor.getValue().paymentIntentId());
    }

    @Test
    void throwsPaymentAlreadySucceededWhenOrderAlreadyHasSucceededPayment() {
        saveVendorAccount(VENDOR_ONE, "acct_1");
        paymentRepositoryPort.save(succeededPayment());

        assertThrows(PaymentAlreadySucceededException.class, () -> initiatePaymentUseCase.execute(command()));

        assertEquals(1, paymentJpaRepository.count());
        verify(paymentGatewayPort, never()).createPaymentIntent(any(), any(), any(), any());
        verify(paymentEventPublisherPort, never()).publishPaymentInitiated(any());
    }

    @Test
    void throwsVendorAccountNotFoundWithoutPersistingPayment() {
        when(paymentGatewayPort.resolveOrCreateCustomer(eq(null), eq("guest@example.com")))
                .thenReturn(STRIPE_CUSTOMER_ID);

        assertThrows(VendorAccountNotFoundException.class, () -> initiatePaymentUseCase.execute(command()));

        assertEquals(0, paymentJpaRepository.count());
        verify(paymentGatewayPort, never()).createPaymentIntent(any(), any(), any(), any());
        verify(paymentEventPublisherPort, never()).publishPaymentInitiated(any());
    }

    private void saveVendorAccount(UUID vendorId, String stripeAccountId) {
        vendorStripeAccountJpaRepository.save(VendorStripeAccountEntity.builder()
                .vendorId(vendorId)
                .stripeAccountId(stripeAccountId)
                .accountStatus(VendorAccountStatus.ACTIVE)
                .chargesEnabled(true)
                .payoutsEnabled(true)
                .build());
    }

    private Payment succeededPayment() {
        Payment payment = Payment.initiate(
                ORDER_ID,
                null,
                "guest@example.com",
                new BigDecimal("150.00"),
                "USD");
        payment.attachPaymentIntent("pi_previous", STRIPE_CUSTOMER_ID);
        payment.markSucceeded("card", "4242", LocalDateTime.now());
        return paymentRepositoryPort.save(payment);
    }

    private InitiatePaymentCommand command() {
        return new InitiatePaymentCommand(
                ORDER_ID,
                null,
                "guest@example.com",
                "guest@example.com",
                new BigDecimal("150.00"),
                "USD",
                List.of(
                        new InitiatePaymentCommand.VendorGrossAmount(VENDOR_ONE, new BigDecimal("100.00")),
                        new InitiatePaymentCommand.VendorGrossAmount(VENDOR_TWO, new BigDecimal("50.00"))));
    }
}