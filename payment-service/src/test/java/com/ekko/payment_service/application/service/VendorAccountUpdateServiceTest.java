package com.ekko.payment_service.application.service;

import com.ekko.payment_service.domain.exception.VendorAccountNotFoundException;
import com.ekko.payment_service.domain.model.HandleAccountUpdatedCommand;
import com.ekko.payment_service.domain.model.VendorAccountStatus;
import com.ekko.payment_service.domain.model.VendorStripeAccount;
import com.ekko.payment_service.domain.port.out.VendorStripeAccountRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorAccountUpdateServiceTest {

    private static final String STRIPE_ACCOUNT_ID = "acct_1";
    private static final UUID VENDOR_ID = UUID.randomUUID();

    @Mock
    private VendorStripeAccountRepositoryPort vendorStripeAccountRepositoryPort;
    @Mock
    private VendorAccountTransactionService vendorAccountTransactionService;

    private VendorAccountUpdateService service;

    @BeforeEach
    void setUp() {
        service = new VendorAccountUpdateService(
                vendorStripeAccountRepositoryPort,
                vendorAccountTransactionService);
    }

    @Test
    void disabledWinsOverActiveWhenDisabledReasonPresent() {
        accountInRepo(VendorAccountStatus.PENDING);
        when(vendorAccountTransactionService.updateAccountStatus(anyAccount()))
                .thenAnswer(inv -> inv.getArgument(0));

        VendorStripeAccount result = service.execute(handleAccountUpdated(
                true, true, true, true));

        assertEquals(VendorAccountStatus.DISABLED, result.getAccountStatus());
        assertEquals(true, result.isChargesEnabled());
        assertEquals(true, result.isPayoutsEnabled());
        assertPersistedAccountHasStatus(VendorAccountStatus.DISABLED);
    }

    @Test
    void activeWhenChargesAndPayoutsEnabledWithoutDisableReason() {
        accountInRepo(VendorAccountStatus.PENDING);
        when(vendorAccountTransactionService.updateAccountStatus(anyAccount()))
                .thenAnswer(inv -> inv.getArgument(0));

        VendorStripeAccount result = service.execute(handleAccountUpdated(
                true, true, false, false));

        assertEquals(VendorAccountStatus.ACTIVE, result.getAccountStatus());
        assertPersistedAccountHasStatus(VendorAccountStatus.ACTIVE);
    }

    @Test
    void restrictedWhenChargesDisabledAndPendingRequirementsPresent() {
        accountInRepo(VendorAccountStatus.PENDING);
        when(vendorAccountTransactionService.updateAccountStatus(anyAccount()))
                .thenAnswer(inv -> inv.getArgument(0));

        VendorStripeAccount result = service.execute(handleAccountUpdated(
                false, true, false, true));

        assertEquals(VendorAccountStatus.RESTRICTED, result.getAccountStatus());
        assertPersistedAccountHasStatus(VendorAccountStatus.RESTRICTED);
    }

    @Test
    void staysPendingWhenNothingEnabledAndNoPendingRequirements() {
        accountInRepo(VendorAccountStatus.PENDING);
        when(vendorAccountTransactionService.updateAccountStatus(anyAccount()))
                .thenAnswer(inv -> inv.getArgument(0));

        VendorStripeAccount result = service.execute(handleAccountUpdated(
                false, false, false, false));

        assertEquals(VendorAccountStatus.PENDING, result.getAccountStatus());
        assertPersistedAccountHasStatus(VendorAccountStatus.PENDING);
    }

    @Test
    void throwsWhenStripeAccountNotFound() {
        when(vendorStripeAccountRepositoryPort.findByStripeAccountId(STRIPE_ACCOUNT_ID))
                .thenReturn(Optional.empty());

        assertThrows(VendorAccountNotFoundException.class,
                () -> service.execute(handleAccountUpdated(true, true, false, false)));
    }

    @Test
    void persistenceHappensAfterTransitionIsApplied() {
        accountInRepo(VendorAccountStatus.PENDING);
        when(vendorAccountTransactionService.updateAccountStatus(anyAccount()))
                .thenAnswer(inv -> inv.getArgument(0));

        service.execute(handleAccountUpdated(true, true, false, false));

        ArgumentCaptor<VendorStripeAccount> captor = ArgumentCaptor.forClass(VendorStripeAccount.class);
        verify(vendorAccountTransactionService).updateAccountStatus(captor.capture());
        VendorStripeAccount persisted = captor.getValue();
        assertEquals(VendorAccountStatus.ACTIVE, persisted.getAccountStatus());
        assertEquals(true, persisted.isChargesEnabled());
        assertEquals(true, persisted.isPayoutsEnabled());
    }

    private void accountInRepo(VendorAccountStatus status) {
        when(vendorStripeAccountRepositoryPort.findByStripeAccountId(STRIPE_ACCOUNT_ID))
                .thenReturn(Optional.of(VendorStripeAccount.restore(
                        UUID.randomUUID(),
                        VENDOR_ID,
                        STRIPE_ACCOUNT_ID,
                        status,
                        status == VendorAccountStatus.ACTIVE,
                        status == VendorAccountStatus.ACTIVE,
                        LocalDateTime.now(),
                        LocalDateTime.now())));
    }

    private HandleAccountUpdatedCommand handleAccountUpdated(boolean chargesEnabled, boolean payoutsEnabled,
                                                             boolean disabledReasonPresent, boolean hasPendingRequirements) {
        return new HandleAccountUpdatedCommand(
                STRIPE_ACCOUNT_ID,
                chargesEnabled,
                payoutsEnabled,
                disabledReasonPresent,
                hasPendingRequirements);
    }

    private VendorStripeAccount anyAccount() {
        return org.mockito.ArgumentMatchers.any(VendorStripeAccount.class);
    }

    private void assertPersistedAccountHasStatus(VendorAccountStatus status) {
        ArgumentCaptor<VendorStripeAccount> captor = ArgumentCaptor.forClass(VendorStripeAccount.class);
        verify(vendorAccountTransactionService).updateAccountStatus(captor.capture());
        assertEquals(status, captor.getValue().getAccountStatus());
    }
}