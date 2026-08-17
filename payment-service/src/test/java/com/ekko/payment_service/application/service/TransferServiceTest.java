package com.ekko.payment_service.application.service;

import com.ekko.payment_service.domain.enums.PaymentStatus;
import com.ekko.payment_service.domain.enums.TransferStatus;
import com.ekko.payment_service.domain.enums.VendorAccountStatus;
import com.ekko.payment_service.domain.event.TransferFailedEvent;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.model.PaymentTransfer;
import com.ekko.payment_service.domain.model.VendorAllocation;
import com.ekko.payment_service.domain.model.VendorStripeAccount;
import com.ekko.payment_service.domain.port.out.PaymentEventPublisherPort;
import com.ekko.payment_service.domain.port.out.PaymentGatewayPort;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.domain.port.out.PaymentTransferRepositoryPort;
import com.ekko.payment_service.domain.port.out.VendorStripeAccountRepositoryPort;
import com.ekko.payment_service.infrastructure.gateway.PaymentGatewayException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final UUID VENDOR_ONE = UUID.randomUUID();
    private static final UUID VENDOR_TWO = UUID.randomUUID();
    private static final BigDecimal NET_AMOUNT_ONE = new BigDecimal("40.00");
    private static final BigDecimal NET_AMOUNT_TWO = new BigDecimal("55.00");

    @Mock
    private PaymentTransferRepositoryPort paymentTransferRepositoryPort;
    @Mock
    private PaymentRepositoryPort paymentRepositoryPort;
    @Mock
    private VendorStripeAccountRepositoryPort vendorStripeAccountRepositoryPort;
    @Mock
    private PaymentGatewayPort paymentGatewayPort;
    @Mock
    private PaymentEventPublisherPort paymentEventPublisherPort;

    private TransferService service;

    @BeforeEach
    void setUp() {
        service = new TransferService(
                paymentTransferRepositoryPort,
                paymentRepositoryPort,
                vendorStripeAccountRepositoryPort,
                paymentGatewayPort,
                paymentEventPublisherPort);
    }

    @Test
    void createsSuccessfulTransfersForVendorsWithChargesEnabledAccounts() {
        Payment payment = paymentWith(VENDOR_ONE, VENDOR_TWO);
        when(paymentTransferRepositoryPort.findByPaymentId(PAYMENT_ID)).thenReturn(List.of());
        when(paymentRepositoryPort.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(vendorStripeAccountRepositoryPort.findByVendorId(VENDOR_ONE))
                .thenReturn(Optional.of(vendorAccount(VENDOR_ONE, "acct_1", true)));
        when(vendorStripeAccountRepositoryPort.findByVendorId(VENDOR_TWO))
                .thenReturn(Optional.of(vendorAccount(VENDOR_TWO, "acct_2", true)));
        when(paymentGatewayPort.createTransfer(any(), any(), any(), any()))
                .thenReturn("tr_1")
                .thenReturn("tr_2");
        when(paymentTransferRepositoryPort.save(any(PaymentTransfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.execute(PAYMENT_ID);

        ArgumentCaptor<PaymentTransfer> transferCaptor = ArgumentCaptor.forClass(PaymentTransfer.class);
        verify(paymentTransferRepositoryPort, org.mockito.Mockito.times(2)).save(transferCaptor.capture());
        List<PaymentTransfer> transfers = transferCaptor.getAllValues();
        assertEquals(TransferStatus.SUCCEEDED, transfers.get(0).getStatus());
        assertEquals("tr_1", transfers.get(0).getStripeTransferId());
        assertEquals(NET_AMOUNT_ONE, transfers.get(0).getAmount());
        assertEquals(TransferStatus.SUCCEEDED, transfers.get(1).getStatus());
        assertEquals("tr_2", transfers.get(1).getStripeTransferId());
        assertEquals(NET_AMOUNT_TWO, transfers.get(1).getAmount());

        verify(paymentGatewayPort).createTransfer(NET_AMOUNT_ONE, "USD", "acct_1", PAYMENT_ID.toString());
        verify(paymentGatewayPort).createTransfer(NET_AMOUNT_TWO, "USD", "acct_2", PAYMENT_ID.toString());
        verifyNoInteractions(paymentEventPublisherPort);
    }

    @Test
    void marksTransferFailedWhenVendorHasNoStripeAccount() {
        Payment payment = paymentWith(VENDOR_ONE, VENDOR_TWO);
        when(paymentTransferRepositoryPort.findByPaymentId(PAYMENT_ID)).thenReturn(List.of());
        when(paymentRepositoryPort.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(vendorStripeAccountRepositoryPort.findByVendorId(VENDOR_ONE))
                .thenReturn(Optional.of(vendorAccount(VENDOR_ONE, "acct_1", true)));
        when(vendorStripeAccountRepositoryPort.findByVendorId(VENDOR_TWO))
                .thenReturn(Optional.empty());
        when(paymentGatewayPort.createTransfer(any(), any(), any(), any())).thenReturn("tr_1");
        when(paymentTransferRepositoryPort.save(any(PaymentTransfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.execute(PAYMENT_ID);

        ArgumentCaptor<PaymentTransfer> transferCaptor = ArgumentCaptor.forClass(PaymentTransfer.class);
        verify(paymentTransferRepositoryPort, org.mockito.Mockito.times(2)).save(transferCaptor.capture());
        List<PaymentTransfer> transfers = transferCaptor.getAllValues();
        assertEquals(TransferStatus.SUCCEEDED, transfers.get(0).getStatus());
        assertEquals(TransferStatus.FAILED, transfers.get(1).getStatus());
        verify(paymentGatewayPort, never()).createTransfer(NET_AMOUNT_TWO, "USD", "acct_2", PAYMENT_ID.toString());
    }

    @Test
    void publishesTransferFailedWhenVendorHasNoStripeAccount() {
        Payment payment = paymentWith(VENDOR_ONE, VENDOR_TWO);
        when(paymentTransferRepositoryPort.findByPaymentId(PAYMENT_ID)).thenReturn(List.of());
        when(paymentRepositoryPort.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(vendorStripeAccountRepositoryPort.findByVendorId(VENDOR_ONE))
                .thenReturn(Optional.of(vendorAccount(VENDOR_ONE, "acct_1", true)));
        when(vendorStripeAccountRepositoryPort.findByVendorId(VENDOR_TWO))
                .thenReturn(Optional.empty());
        when(paymentGatewayPort.createTransfer(any(), any(), any(), any())).thenReturn("tr_1");
        when(paymentTransferRepositoryPort.save(any(PaymentTransfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.execute(PAYMENT_ID);

        ArgumentCaptor<TransferFailedEvent> eventCaptor = ArgumentCaptor.forClass(TransferFailedEvent.class);
        verify(paymentEventPublisherPort, org.mockito.Mockito.times(1)).publishTransferFailed(eventCaptor.capture());
        TransferFailedEvent event = eventCaptor.getValue();
        assertEquals(PAYMENT_ID, event.paymentId());
        assertEquals(payment.getOrderId(), event.orderId());
        assertEquals(VENDOR_TWO, event.vendorId());
        assertEquals(NET_AMOUNT_TWO, event.amount());
        assertEquals("USD", event.currency());
    }

    @Test
    void publishesTransferFailedWhenGatewayThrows() {
        Payment payment = paymentWith(VENDOR_ONE, VENDOR_TWO);
        when(paymentTransferRepositoryPort.findByPaymentId(PAYMENT_ID)).thenReturn(List.of());
        when(paymentRepositoryPort.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(vendorStripeAccountRepositoryPort.findByVendorId(VENDOR_ONE))
                .thenReturn(Optional.of(vendorAccount(VENDOR_ONE, "acct_1", true)));
        when(vendorStripeAccountRepositoryPort.findByVendorId(VENDOR_TWO))
                .thenReturn(Optional.of(vendorAccount(VENDOR_TWO, "acct_2", true)));
        when(paymentGatewayPort.createTransfer(any(), any(), any(), any()))
                .thenThrow(new PaymentGatewayException("boom"))
                .thenReturn("tr_2");
        when(paymentTransferRepositoryPort.save(any(PaymentTransfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.execute(PAYMENT_ID);

        ArgumentCaptor<TransferFailedEvent> eventCaptor = ArgumentCaptor.forClass(TransferFailedEvent.class);
        verify(paymentEventPublisherPort, org.mockito.Mockito.times(1)).publishTransferFailed(eventCaptor.capture());
        TransferFailedEvent event = eventCaptor.getValue();
        assertEquals(PAYMENT_ID, event.paymentId());
        assertEquals(payment.getOrderId(), event.orderId());
        assertEquals(VENDOR_ONE, event.vendorId());
        assertEquals(NET_AMOUNT_ONE, event.amount());
        assertEquals("USD", event.currency());
    }

    @Test
    void marksTransferFailedWhenVendorAccountDoesNotHaveChargesEnabled() {
        Payment payment = paymentWith(VENDOR_ONE, VENDOR_TWO);
        when(paymentTransferRepositoryPort.findByPaymentId(PAYMENT_ID)).thenReturn(List.of());
        when(paymentRepositoryPort.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(vendorStripeAccountRepositoryPort.findByVendorId(VENDOR_ONE))
                .thenReturn(Optional.of(vendorAccount(VENDOR_ONE, "acct_1", false)));
        when(vendorStripeAccountRepositoryPort.findByVendorId(VENDOR_TWO))
                .thenReturn(Optional.of(vendorAccount(VENDOR_TWO, "acct_2", true)));
        when(paymentGatewayPort.createTransfer(any(), any(), any(), any())).thenReturn("tr_2");
        when(paymentTransferRepositoryPort.save(any(PaymentTransfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.execute(PAYMENT_ID);

        ArgumentCaptor<PaymentTransfer> transferCaptor = ArgumentCaptor.forClass(PaymentTransfer.class);
        verify(paymentTransferRepositoryPort, org.mockito.Mockito.times(2)).save(transferCaptor.capture());
        List<PaymentTransfer> transfers = transferCaptor.getAllValues();
        assertEquals(TransferStatus.FAILED, transfers.get(0).getStatus());
        assertEquals(TransferStatus.SUCCEEDED, transfers.get(1).getStatus());
        verify(paymentTransferRepositoryPort).save(transfers.get(0));
    }

    @Test
    void marksTransferFailedWhenGatewayThrows() {
        Payment payment = paymentWith(VENDOR_ONE, VENDOR_TWO);
        when(paymentTransferRepositoryPort.findByPaymentId(PAYMENT_ID)).thenReturn(List.of());
        when(paymentRepositoryPort.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(vendorStripeAccountRepositoryPort.findByVendorId(VENDOR_ONE))
                .thenReturn(Optional.of(vendorAccount(VENDOR_ONE, "acct_1", true)));
        when(vendorStripeAccountRepositoryPort.findByVendorId(VENDOR_TWO))
                .thenReturn(Optional.of(vendorAccount(VENDOR_TWO, "acct_2", true)));
        when(paymentGatewayPort.createTransfer(any(), any(), any(), any()))
                .thenThrow(new PaymentGatewayException("boom"))
                .thenReturn("tr_2");
        when(paymentTransferRepositoryPort.save(any(PaymentTransfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.execute(PAYMENT_ID);

        ArgumentCaptor<PaymentTransfer> transferCaptor = ArgumentCaptor.forClass(PaymentTransfer.class);
        verify(paymentTransferRepositoryPort, org.mockito.Mockito.times(2)).save(transferCaptor.capture());
        assertEquals(TransferStatus.FAILED, transferCaptor.getAllValues().get(0).getStatus());
        assertEquals(TransferStatus.SUCCEEDED, transferCaptor.getAllValues().get(1).getStatus());
    }

    @Test
    void doesNotProcessWhenAnyTransferAlreadyExists() {
        when(paymentTransferRepositoryPort.findByPaymentId(PAYMENT_ID))
                .thenReturn(List.of(PaymentTransfer.restore(
                        UUID.randomUUID(),
                        PAYMENT_ID,
                        VENDOR_ONE,
                        "tr_1",
                        NET_AMOUNT_ONE,
                        "USD",
                        TransferStatus.SUCCEEDED,
                        LocalDateTime.now(),
                        LocalDateTime.now())));

        service.execute(PAYMENT_ID);

        verifyNoInteractions(paymentRepositoryPort);
        verifyNoInteractions(paymentGatewayPort);
        verifyNoInteractions(paymentEventPublisherPort);
    }

    @Test
    void throwsWhenPaymentNotFound() {
        when(paymentTransferRepositoryPort.findByPaymentId(PAYMENT_ID)).thenReturn(List.of());
        when(paymentRepositoryPort.findById(PAYMENT_ID)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.execute(PAYMENT_ID));
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

    private Payment paymentWith(UUID... vendorIds) {
        List<VendorAllocation> allocations = java.util.Arrays.stream(vendorIds)
                .map(vendorId -> new VendorAllocation(vendorId, new BigDecimal("60.00"), new BigDecimal("20.00"),
                        vendorId.equals(VENDOR_ONE) ? NET_AMOUNT_ONE : NET_AMOUNT_TWO))
                .toList();
        return Payment.restore(
                PAYMENT_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "guest@example.com",
                "pi_test_123",
                new BigDecimal("110.00"),
                "USD",
                PaymentStatus.SUCCEEDED,
                "cus_123",
                "card",
                "4242",
                LocalDateTime.now(),
                allocations,
                LocalDateTime.now(),
                LocalDateTime.now());
    }
}