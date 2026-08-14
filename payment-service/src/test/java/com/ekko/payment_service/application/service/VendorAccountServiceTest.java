package com.ekko.payment_service.application.service;

import com.ekko.payment_service.domain.exception.VendorAccountAlreadyActiveException;
import com.ekko.payment_service.domain.exception.VendorAccountAlreadyExistsException;
import com.ekko.payment_service.domain.exception.VendorAccountNotFoundException;
import com.ekko.payment_service.domain.model.CreateVendorAccountCommand;
import com.ekko.payment_service.domain.model.VendorAccountResult;
import com.ekko.payment_service.domain.model.VendorAccountStatus;
import com.ekko.payment_service.domain.model.VendorStripeAccount;
import com.ekko.payment_service.domain.port.out.PaymentGatewayPort;
import com.ekko.payment_service.domain.port.out.VendorStripeAccountRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorAccountServiceTest {

    private static final UUID VENDOR_ID = UUID.randomUUID();
    private static final String COUNTRY = "US";
    private static final String EMAIL = "vendor@example.com";
    private static final String STRIPE_ACCOUNT_ID = "acct_1";
    private static final String ONBOARDING_URL = "https://connect.stripe.com/onboarding/url";
    private static final String REFRESH_URL = "https://ekko.app/vendor/onboarding/refresh";
    private static final String RETURN_URL = "https://ekko.app/vendor/onboarding/return";

    @Mock
    private VendorStripeAccountRepositoryPort vendorStripeAccountRepositoryPort;
    @Mock
    private PaymentGatewayPort paymentGatewayPort;
    @Mock
    private VendorAccountTransactionService vendorAccountTransactionService;

    private VendorAccountService service;

    @BeforeEach
    void setUp() {
        service = new VendorAccountService(
                vendorStripeAccountRepositoryPort,
                paymentGatewayPort,
                vendorAccountTransactionService,
                REFRESH_URL,
                RETURN_URL);
    }

    @Test
    void createVendorAccountExecutesExpectedOrder() {
        when(vendorStripeAccountRepositoryPort.findByVendorId(VENDOR_ID)).thenReturn(Optional.empty());
        when(paymentGatewayPort.createConnectedAccount(COUNTRY, EMAIL)).thenReturn(STRIPE_ACCOUNT_ID);
        when(paymentGatewayPort.createAccountLink(STRIPE_ACCOUNT_ID, REFRESH_URL, RETURN_URL))
                .thenReturn(ONBOARDING_URL);
        when(vendorAccountTransactionService.commitVendorAccount(any(VendorStripeAccount.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        VendorAccountResult result = service.execute(new CreateVendorAccountCommand(VENDOR_ID, COUNTRY, EMAIL));

        assertEquals(VENDOR_ID, result.vendorStripeAccount().getVendorId());
        assertEquals(STRIPE_ACCOUNT_ID, result.vendorStripeAccount().getStripeAccountId());
        assertEquals(VendorAccountStatus.PENDING, result.vendorStripeAccount().getAccountStatus());
        assertEquals(ONBOARDING_URL, result.onboardingUrl());

        InOrder inOrder = inOrder(
                vendorStripeAccountRepositoryPort,
                paymentGatewayPort,
                vendorAccountTransactionService);
        inOrder.verify(vendorStripeAccountRepositoryPort).findByVendorId(VENDOR_ID);
        inOrder.verify(paymentGatewayPort).createConnectedAccount(COUNTRY, EMAIL);
        inOrder.verify(paymentGatewayPort).createAccountLink(STRIPE_ACCOUNT_ID, REFRESH_URL, RETURN_URL);
        inOrder.verify(vendorAccountTransactionService).commitVendorAccount(any(VendorStripeAccount.class));
    }

    @Test
    void createVendorAccountAbortsEarlyWhenAccountAlreadyExists() {
        when(vendorStripeAccountRepositoryPort.findByVendorId(VENDOR_ID))
                .thenReturn(Optional.of(vendorAccount(VendorAccountStatus.PENDING)));

        assertThrows(VendorAccountAlreadyExistsException.class,
                () -> service.execute(new CreateVendorAccountCommand(VENDOR_ID, COUNTRY, EMAIL)));

        verify(paymentGatewayPort, never()).createConnectedAccount(any(), any());
        verify(paymentGatewayPort, never()).createAccountLink(any(), any(), any());
        verifyNoInteractions(vendorAccountTransactionService);
    }

    @Test
    void refreshOnboardingLinkReturnsNewUrlWithoutPersisting() {
        when(vendorStripeAccountRepositoryPort.findByVendorId(VENDOR_ID))
                .thenReturn(Optional.of(vendorAccount(VendorAccountStatus.PENDING)));
        when(paymentGatewayPort.createAccountLink(STRIPE_ACCOUNT_ID, REFRESH_URL, RETURN_URL))
                .thenReturn(ONBOARDING_URL);

        String url = service.execute(VENDOR_ID);

        assertEquals(ONBOARDING_URL, url);
        verifyNoInteractions(vendorAccountTransactionService);
    }

    @Test
    void refreshOnboardingLinkThrowsWhenVendorAccountNotFound() {
        when(vendorStripeAccountRepositoryPort.findByVendorId(VENDOR_ID)).thenReturn(Optional.empty());

        assertThrows(VendorAccountNotFoundException.class, () -> service.execute(VENDOR_ID));

        verify(paymentGatewayPort, never()).createAccountLink(any(), any(), any());
    }

    @Test
    void refreshOnboardingLinkThrowsWhenAccountAlreadyActive() {
        when(vendorStripeAccountRepositoryPort.findByVendorId(VENDOR_ID))
                .thenReturn(Optional.of(vendorAccount(VendorAccountStatus.ACTIVE)));

        assertThrows(VendorAccountAlreadyActiveException.class, () -> service.execute(VENDOR_ID));

        verify(paymentGatewayPort, never()).createAccountLink(any(), any(), any());
    }

    private VendorStripeAccount vendorAccount(VendorAccountStatus status) {
        return VendorStripeAccount.restore(
                UUID.randomUUID(),
                VENDOR_ID,
                STRIPE_ACCOUNT_ID,
                status,
                status == VendorAccountStatus.ACTIVE,
                status == VendorAccountStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now());
    }
}