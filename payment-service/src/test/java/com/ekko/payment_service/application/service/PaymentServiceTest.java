package com.ekko.payment_service.application.service;

import com.ekko.payment_service.domain.exception.PaymentAlreadySucceededException;
import com.ekko.payment_service.domain.exception.VendorAccountNotActiveException;
import com.ekko.payment_service.application.exception.VendorAccountNotFoundException;
import com.ekko.payment_service.domain.command.InitiatePaymentCommand;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.event.PaymentInitiatedEvent;
import com.ekko.payment_service.domain.enums.PaymentStatus;
import com.ekko.payment_service.domain.enums.VendorAccountStatus;
import com.ekko.payment_service.domain.model.VendorAllocation;
import com.ekko.payment_service.domain.model.VendorStripeAccount;
import com.ekko.payment_service.domain.port.out.PaymentEventPublisherPort;
import com.ekko.payment_service.domain.port.out.PaymentGatewayPort;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.domain.port.out.VendorStripeAccountRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID VENDOR_ID = UUID.randomUUID();
    private static final BigDecimal AMOUNT = new BigDecimal("150.00");

    @Mock
    private PaymentRepositoryPort paymentRepositoryPort;
    @Mock
    private VendorStripeAccountRepositoryPort vendorStripeAccountRepositoryPort;
    @Mock
    private PaymentGatewayPort paymentGatewayPort;
    @Mock
    private PaymentEventPublisherPort paymentEventPublisherPort;
    @Mock
    private PaymentTransactionService paymentTransactionService;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepositoryPort,
                vendorStripeAccountRepositoryPort,
                paymentGatewayPort,
                paymentEventPublisherPort,
                paymentTransactionService);
    }

    @Test
    void executesHappyPathInExpectedOrder() {
        when(paymentRepositoryPort.findByOrderIdAndStatus(ORDER_ID, PaymentStatus.SUCCEEDED))
                .thenReturn(Optional.empty());
        when(paymentRepositoryPort.findExistingStripeCustomerId(CUSTOMER_ID))
                .thenReturn(Optional.of("cus_existing"));
        when(paymentGatewayPort.resolveOrCreateCustomer("cus_existing", "customer@example.com"))
                .thenReturn("cus_existing");
        when(vendorStripeAccountRepositoryPort.findByVendorId(VENDOR_ID))
                .thenReturn(Optional.of(vendorAccount(VENDOR_ID, "acct_1", true)));
        when(paymentGatewayPort.createPaymentIntent(AMOUNT, "USD", "cus_existing", ORDER_ID.toString()))
                .thenReturn("pi_123");
        when(paymentTransactionService.commitPayment(any(Payment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.execute(command(List.of(
                new InitiatePaymentCommand.VendorGrossAmount(VENDOR_ID, new BigDecimal("100.00")))));

        assertEquals("pi_123", result.getPaymentIntentId());
        assertEquals("cus_existing", result.getStripeCustomerId());
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        assertEquals(1, result.getAllocations().size());
        VendorAllocation allocation = result.getAllocations().get(0);
        assertEquals(0, new BigDecimal("90.00").compareTo(allocation.netAmount()));

        InOrder inOrder = inOrder(
                paymentRepositoryPort,
                paymentGatewayPort,
                vendorStripeAccountRepositoryPort,
                paymentTransactionService,
                paymentEventPublisherPort);
        inOrder.verify(paymentRepositoryPort).findByOrderIdAndStatus(ORDER_ID, PaymentStatus.SUCCEEDED);
        inOrder.verify(paymentRepositoryPort).findExistingStripeCustomerId(CUSTOMER_ID);
        inOrder.verify(paymentGatewayPort).resolveOrCreateCustomer("cus_existing", "customer@example.com");
        inOrder.verify(vendorStripeAccountRepositoryPort).findByVendorId(VENDOR_ID);
        inOrder.verify(paymentGatewayPort).createPaymentIntent(AMOUNT, "USD", "cus_existing", ORDER_ID.toString());
        inOrder.verify(paymentTransactionService).commitPayment(any(Payment.class));
        inOrder.verify(paymentEventPublisherPort).publishPaymentInitiated(any(PaymentInitiatedEvent.class));
    }

    @Test
    void throwsPaymentAlreadySucceededWhenOrderHasSucceededPayment() {
        when(paymentRepositoryPort.findByOrderIdAndStatus(ORDER_ID, PaymentStatus.SUCCEEDED))
                .thenReturn(Optional.of(succeededPayment()));

        assertThrows(PaymentAlreadySucceededException.class,
                () -> paymentService.execute(command(List.of())));

        verify(paymentGatewayPort, never()).resolveOrCreateCustomer(any(), any());
        verify(paymentGatewayPort, never()).createPaymentIntent(any(), any(), any(), any());
        verify(paymentTransactionService, never()).commitPayment(any());
    }

    @Test
    void throwsVendorAccountNotFoundAndNeverCreatesPaymentIntent() {
        when(paymentRepositoryPort.findByOrderIdAndStatus(ORDER_ID, PaymentStatus.SUCCEEDED))
                .thenReturn(Optional.empty());
        when(vendorStripeAccountRepositoryPort.findByVendorId(VENDOR_ID))
                .thenReturn(Optional.empty());

        VendorAccountNotFoundException ex = assertThrows(VendorAccountNotFoundException.class,
                () -> paymentService.execute(command(List.of(
                        new InitiatePaymentCommand.VendorGrossAmount(VENDOR_ID, new BigDecimal("100.00"))))));

        assertEquals(VENDOR_ID, ex.getVendorId());
        verify(paymentGatewayPort, never()).createPaymentIntent(any(), any(), any(), any());
        verify(paymentTransactionService, never()).commitPayment(any());
    }

    @Test
    void throwsVendorAccountNotActiveWhenChargesDisabled() {
        when(paymentRepositoryPort.findByOrderIdAndStatus(ORDER_ID, PaymentStatus.SUCCEEDED))
                .thenReturn(Optional.empty());
        when(vendorStripeAccountRepositoryPort.findByVendorId(VENDOR_ID))
                .thenReturn(Optional.of(vendorAccount(VENDOR_ID, "acct_1", false)));

        VendorAccountNotActiveException ex = assertThrows(VendorAccountNotActiveException.class,
                () -> paymentService.execute(command(List.of(
                        new InitiatePaymentCommand.VendorGrossAmount(VENDOR_ID, new BigDecimal("100.00"))))));

        assertEquals(VENDOR_ID, ex.getVendorId());
        verify(paymentGatewayPort, never()).createPaymentIntent(any(), any(), any(), any());
    }

    @Test
    void guestWithoutCustomerIdPassesNullAndSkipsExistingCustomerLookup() {
        when(paymentRepositoryPort.findByOrderIdAndStatus(ORDER_ID, PaymentStatus.SUCCEEDED))
                .thenReturn(Optional.empty());
        when(paymentGatewayPort.resolveOrCreateCustomer(null, "guest@example.com"))
                .thenReturn("cus_guest");
        when(vendorStripeAccountRepositoryPort.findByVendorId(VENDOR_ID))
                .thenReturn(Optional.of(vendorAccount(VENDOR_ID, "acct_1", true)));
        when(paymentGatewayPort.createPaymentIntent(AMOUNT, "USD", "cus_guest", ORDER_ID.toString()))
                .thenReturn("pi_123");
        when(paymentTransactionService.commitPayment(any(Payment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.execute(
                new InitiatePaymentCommand(
                        ORDER_ID,
                        null,
                        "guest@example.com",
                        "guest@example.com",
                        AMOUNT,
                        "USD",
                        List.of(new InitiatePaymentCommand.VendorGrossAmount(VENDOR_ID, new BigDecimal("100.00")))));

        verify(paymentRepositoryPort, never()).findExistingStripeCustomerId(any());
        assertEquals("cus_guest", result.getStripeCustomerId());

        ArgumentCaptor<PaymentInitiatedEvent> captor = ArgumentCaptor.forClass(PaymentInitiatedEvent.class);
        verify(paymentEventPublisherPort).publishPaymentInitiated(captor.capture());
        assertNull(captor.getValue().customerId());
        assertTrue(captor.getValue().orderId().equals(ORDER_ID));
    }

    private VendorStripeAccount vendorAccount(UUID vendorId, String stripeAccountId, boolean chargesEnabled) {
        return VendorStripeAccount.restore(
                UUID.randomUUID(),
                vendorId,
                stripeAccountId,
                chargesEnabled ? VendorAccountStatus.ACTIVE : VendorAccountStatus.PENDING,
                chargesEnabled,
                false,
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    private InitiatePaymentCommand command(List<InitiatePaymentCommand.VendorGrossAmount> vendors) {
        return new InitiatePaymentCommand(
                ORDER_ID,
                CUSTOMER_ID,
                "guest@example.com",
                "customer@example.com",
                AMOUNT,
                "USD",
                vendors);
    }

    private Payment succeededPayment() {
        return Payment.restore(
                UUID.randomUUID(),
                ORDER_ID,
                CUSTOMER_ID,
                "guest@example.com",
                "pi_123",
                AMOUNT,
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
}