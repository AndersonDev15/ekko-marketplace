package com.ekko.payment_service.application.service;

import com.ekko.payment_service.config.IntegrationTestConfig;
import com.ekko.payment_service.domain.command.InitiatePaymentCommand;
import com.ekko.payment_service.domain.exception.PaymentAlreadySucceededException;
import com.ekko.payment_service.application.exception.VendorAccountNotFoundException;
import com.ekko.payment_service.domain.exception.VendorAccountNotActiveException;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.event.PaymentInitiatedEvent;
import com.ekko.payment_service.domain.enums.PaymentStatus;
import com.ekko.payment_service.infrastructure.persistence.enums.VendorAccountStatus;
import com.ekko.payment_service.domain.port.in.InitiatePaymentUseCase;
import com.ekko.payment_service.domain.port.out.PaymentEventPublisherPort;
import com.ekko.payment_service.domain.port.out.PaymentGatewayPort;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.infrastructure.persistence.entity.PaymentEntity;
import com.ekko.payment_service.infrastructure.persistence.entity.VendorAllocationEntity;
import com.ekko.payment_service.infrastructure.persistence.entity.VendorStripeAccountEntity;
import com.ekko.payment_service.infrastructure.persistence.repository.PaymentJpaRepository;
import com.ekko.payment_service.infrastructure.persistence.repository.VendorStripeAccountJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ContextConfiguration(classes = IntegrationTestConfig.class)
@Import(IntegrationTestConfig.class)
@Transactional
class PaymentServiceIntegrationTest {

    private static final UUID ORDER_ID = UUID.randomUUID();
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

    @MockitoBean
    private PaymentGatewayPort paymentGatewayPort;

    @MockitoBean
    private PaymentEventPublisherPort paymentEventPublisherPort;

    @AfterEach
    void cleanDatabase() {
        paymentJpaRepository.deleteAll();
        vendorStripeAccountJpaRepository.deleteAll();
    }

    @Test
    void happyPathInitiatesPaymentAndPersistsPendingPaymentWithAllocations() {
        UUID vendorOne = UUID.randomUUID();
        UUID vendorTwo = UUID.randomUUID();
        saveVendorAccount(vendorOne, "acct_1", true);
        saveVendorAccount(vendorTwo, "acct_2", true);

        when(paymentGatewayPort.resolveOrCreateCustomer(eq(null), eq("guest@example.com")))
                .thenReturn(STRIPE_CUSTOMER_ID);
        when(paymentGatewayPort.createPaymentIntent(any(BigDecimal.class), eq("USD"), eq(STRIPE_CUSTOMER_ID),
                eq(ORDER_ID.toString())))
                .thenReturn(PAYMENT_INTENT_ID);

        Payment result = initiatePaymentUseCase.execute(command(vendorOne, vendorTwo));

        assertNotNull(result.getId());
        assertEquals(ORDER_ID, result.getOrderId());
        assertEquals(new BigDecimal("150.00"), result.getAmount());
        assertEquals(PAYMENT_INTENT_ID, result.getPaymentIntentId());
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        assertEquals(2, result.getAllocations().size());

        PaymentEntity persisted = paymentJpaRepository.findByPaymentIntentId(PAYMENT_INTENT_ID).orElseThrow();
        assertEquals(PaymentStatus.PENDING, persisted.getStatus());
        assertEquals(PAYMENT_INTENT_ID, persisted.getPaymentIntentId());

        List<VendorAllocationEntity> allocations = persisted.getAllocations();
        assertEquals(2, allocations.size());

        ArgumentCaptor<PaymentInitiatedEvent> captor = ArgumentCaptor.forClass(PaymentInitiatedEvent.class);
        verify(paymentEventPublisherPort, times(1)).publishPaymentInitiated(captor.capture());
        assertEquals(persisted.getId(), captor.getValue().paymentId());
        assertEquals(ORDER_ID, captor.getValue().orderId());
        assertEquals(PAYMENT_INTENT_ID, captor.getValue().paymentIntentId());
    }

    @Test
    void throwsPaymentAlreadySucceededWhenOrderAlreadyHasSucceededPayment() {
        UUID vendorOne = UUID.randomUUID();
        saveVendorAccount(vendorOne, "acct_" + vendorOne.toString().substring(0, 8), true);
        paymentRepositoryPort.save(succeededPayment());

        assertThrows(PaymentAlreadySucceededException.class,
                () -> initiatePaymentUseCase.execute(command(vendorOne, UUID.randomUUID())));

        assertEquals(1, paymentJpaRepository.count());
        verify(paymentGatewayPort, never()).createPaymentIntent(any(), any(), any(), any());
        verify(paymentEventPublisherPort, never()).publishPaymentInitiated(any());
    }

    @Test
    void throwsVendorAccountNotFoundWithoutPersistingPayment() {
        when(paymentGatewayPort.resolveOrCreateCustomer(eq(null), eq("guest@example.com")))
                .thenReturn(STRIPE_CUSTOMER_ID);

        assertThrows(VendorAccountNotFoundException.class,
                () -> initiatePaymentUseCase.execute(command(UUID.randomUUID(), UUID.randomUUID())));

        assertEquals(0, paymentJpaRepository.count());
        verify(paymentGatewayPort, never()).createPaymentIntent(any(), any(), any(), any());
        verify(paymentEventPublisherPort, never()).publishPaymentInitiated(any());
    }

    @Test
    void throwsVendorAccountNotActiveWhenChargesDisabled() {
        UUID vendorOne = UUID.randomUUID();
        saveVendorAccount(vendorOne, "acct_1", false);

        when(paymentGatewayPort.resolveOrCreateCustomer(eq(null), eq("guest@example.com")))
                .thenReturn(STRIPE_CUSTOMER_ID);

        assertThrows(VendorAccountNotActiveException.class,
                () -> initiatePaymentUseCase.execute(command(vendorOne, UUID.randomUUID())));

        assertEquals(0, paymentJpaRepository.count());
        verify(paymentGatewayPort, never()).createPaymentIntent(any(), any(), any(), any());
        verify(paymentEventPublisherPort, never()).publishPaymentInitiated(any());
    }

    @Test
    void guestWithoutCustomerIdPassesNullAndSkipsExistingCustomerLookup() {
        UUID vendorOne = UUID.randomUUID();
        saveVendorAccount(vendorOne, "acct_1", true);

        when(paymentGatewayPort.resolveOrCreateCustomer(eq(null), eq("guest@example.com")))
                .thenReturn(STRIPE_CUSTOMER_ID);
        when(paymentGatewayPort.createPaymentIntent(any(BigDecimal.class), eq("USD"), eq(STRIPE_CUSTOMER_ID),
                eq(ORDER_ID.toString())))
                .thenReturn(PAYMENT_INTENT_ID);

        Payment result = initiatePaymentUseCase.execute(
                new InitiatePaymentCommand(
                        ORDER_ID,
                        null,
                        "guest@example.com",
                        new BigDecimal("150.00"),
                        "USD",
                        List.of(new InitiatePaymentCommand.VendorGrossAmount(vendorOne, new BigDecimal("100.00")))));

        assertEquals(STRIPE_CUSTOMER_ID, result.getStripeCustomerId());

        ArgumentCaptor<PaymentInitiatedEvent> captor = ArgumentCaptor.forClass(PaymentInitiatedEvent.class);
        verify(paymentEventPublisherPort).publishPaymentInitiated(captor.capture());
        assertEquals(null, captor.getValue().customerId());
    }

    @Test
    void customerWithExistingStripeCustomerReusesCustomerId() {
        UUID vendorOne = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        saveVendorAccount(vendorOne, "acct_1", true);

        // Pre-save a payment with this customer ID and stripe customer ID
        Payment existingPayment = Payment.initiate(
                UUID.randomUUID(),
                customerId,
                "customer@example.com",
                new BigDecimal("100.00"),
                "USD");
        existingPayment.attachPaymentIntent("pi_existing", "cus_existing");
        existingPayment.markSucceeded("card", "4242", LocalDateTime.now());
        paymentRepositoryPort.save(existingPayment);

        when(paymentGatewayPort.resolveOrCreateCustomer(eq("cus_existing"), eq("customer@example.com")))
                .thenReturn("cus_existing");
        when(paymentGatewayPort.createPaymentIntent(any(BigDecimal.class), eq("USD"), eq("cus_existing"),
                eq(ORDER_ID.toString())))
                .thenReturn(PAYMENT_INTENT_ID);

        Payment result = initiatePaymentUseCase.execute(
                new InitiatePaymentCommand(
                        ORDER_ID,
                        customerId,
                        "customer@example.com",
                        new BigDecimal("150.00"),
                        "USD",
                        List.of(new InitiatePaymentCommand.VendorGrossAmount(vendorOne, new BigDecimal("100.00")))));

        assertEquals("cus_existing", result.getStripeCustomerId());
    }

    @Test
    void calculatesApplicationFeesCorrectlyForMultipleVendors() {
        UUID vendorOne = UUID.randomUUID();
        UUID vendorTwo = UUID.randomUUID();
        saveVendorAccount(vendorOne, "acct_1", true);
        saveVendorAccount(vendorTwo, "acct_2", true);

        when(paymentGatewayPort.resolveOrCreateCustomer(eq(null), eq("guest@example.com")))
                .thenReturn(STRIPE_CUSTOMER_ID);
        when(paymentGatewayPort.createPaymentIntent(any(BigDecimal.class), eq("USD"), eq(STRIPE_CUSTOMER_ID),
                eq(ORDER_ID.toString())))
                .thenReturn(PAYMENT_INTENT_ID);

        Payment result = initiatePaymentUseCase.execute(command(vendorOne, vendorTwo));

        var allocOne = result.getAllocations().stream()
                .filter(a -> a.vendorId().equals(vendorOne))
                .findFirst().orElseThrow();
        var allocTwo = result.getAllocations().stream()
                .filter(a -> a.vendorId().equals(vendorTwo))
                .findFirst().orElseThrow();

        assertEquals(0, new BigDecimal("100.00").compareTo(allocOne.grossAmount()));
        assertEquals(0, new BigDecimal("10.00").compareTo(allocOne.applicationFeeAmount()));
        assertEquals(0, new BigDecimal("90.00").compareTo(allocOne.netAmount()));

        assertEquals(0, new BigDecimal("50.00").compareTo(allocTwo.grossAmount()));
        assertEquals(0, new BigDecimal("5.00").compareTo(allocTwo.applicationFeeAmount()));
        assertEquals(0, new BigDecimal("45.00").compareTo(allocTwo.netAmount()));
    }

    private void saveVendorAccount(UUID vendorId, String stripeAccountId, boolean chargesEnabled) {
        vendorStripeAccountJpaRepository.save(VendorStripeAccountEntity.builder()
                .vendorId(vendorId)
                .stripeAccountId(stripeAccountId)
                .accountStatus(chargesEnabled ? VendorAccountStatus.ACTIVE : VendorAccountStatus.PENDING)
                .chargesEnabled(chargesEnabled)
                .payoutsEnabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
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

    private InitiatePaymentCommand command(UUID vendorOne, UUID vendorTwo) {
        return new InitiatePaymentCommand(
                ORDER_ID,
                null,
                "guest@example.com",
                new BigDecimal("150.00"),
                "USD",
                List.of(
                        new InitiatePaymentCommand.VendorGrossAmount(vendorOne, new BigDecimal("100.00")),
                        new InitiatePaymentCommand.VendorGrossAmount(vendorTwo, new BigDecimal("50.00"))));
    }
}