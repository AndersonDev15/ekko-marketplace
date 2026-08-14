package com.ekko.payment_service.application.service;

import com.ekko.payment_service.config.AbstractPostgresIntegrationTest;
import com.ekko.payment_service.domain.exception.VendorAccountAlreadyExistsException;
import com.ekko.payment_service.domain.model.CreateVendorAccountCommand;
import com.ekko.payment_service.domain.model.VendorAccountResult;
import com.ekko.payment_service.domain.port.in.CreateVendorAccountUseCase;
import com.ekko.payment_service.domain.port.in.RefreshOnboardingLinkUseCase;
import com.ekko.payment_service.domain.port.out.PaymentGatewayPort;
import com.ekko.payment_service.infrastructure.persistence.entity.VendorAccountStatus;
import com.ekko.payment_service.infrastructure.persistence.entity.VendorStripeAccountEntity;
import com.ekko.payment_service.infrastructure.persistence.repository.VendorStripeAccountJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VendorAccountServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final UUID VENDOR_ID = UUID.randomUUID();
    private static final String COUNTRY = "US";
    private static final String EMAIL = "vendor@example.com";
    private static final String STRIPE_ACCOUNT_ID = "acct_1";
    private static final String ONBOARDING_URL = "https://connect.stripe.com/onboarding/url";
    private static final String REFRESH_URL = "https://ekko.app/vendor/onboarding/refresh";
    private static final String RETURN_URL = "https://ekko.app/vendor/onboarding/return";

    @Autowired
    private CreateVendorAccountUseCase createVendorAccountUseCase;

    @Autowired
    private RefreshOnboardingLinkUseCase refreshOnboardingLinkUseCase;

    @Autowired
    private VendorStripeAccountJpaRepository vendorStripeAccountJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private PaymentGatewayPort paymentGatewayPort;

    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM vendor_stripe_accounts");
    }

    @Test
    void createVendorAccountPersistsPendingAccountWithPopulatedTimestamps() {
        when(paymentGatewayPort.createConnectedAccount(COUNTRY, EMAIL)).thenReturn(STRIPE_ACCOUNT_ID);
        when(paymentGatewayPort.createAccountLink(any(String.class), any(String.class), any(String.class)))
                .thenReturn(ONBOARDING_URL);

        VendorAccountResult result = createVendorAccountUseCase.execute(
                new CreateVendorAccountCommand(VENDOR_ID, COUNTRY, EMAIL));

        assertEquals(STRIPE_ACCOUNT_ID, result.vendorStripeAccount().getStripeAccountId());
        assertEquals(ONBOARDING_URL, result.onboardingUrl());

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT account_status, charges_enabled, payouts_enabled, created_at, updated_at " +
                        "FROM vendor_stripe_accounts WHERE vendor_id = ?",
                VENDOR_ID);
        assertEquals(1, rows.size());
        Map<String, Object> row = rows.get(0);
        assertEquals("PENDING", row.get("account_status"));
        assertEquals(false, row.get("charges_enabled"));
        assertEquals(false, row.get("payouts_enabled"));
        assertNotNull(row.get("created_at"));
        assertNotNull(row.get("updated_at"));
    }

    @Test
    void createVendorAccountThrowsAlreadyExistsWithoutCreatingSecondRow() {
        saveAccount(VendorAccountStatus.PENDING, false, false);
        when(paymentGatewayPort.createConnectedAccount(any(), any())).thenReturn(STRIPE_ACCOUNT_ID);

        assertThrows(VendorAccountAlreadyExistsException.class, () ->
                createVendorAccountUseCase.execute(new CreateVendorAccountCommand(VENDOR_ID, COUNTRY, EMAIL)));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM vendor_stripe_accounts WHERE vendor_id = ?",
                Integer.class, VENDOR_ID);
        assertEquals(1, count);
        verify(paymentGatewayPort, never()).createAccountLink(any(), any(), any());
    }

    @Test
    void refreshOnboardingLinkReturnsUrlWithoutModifyingDatabase() {
        saveAccount(VendorAccountStatus.PENDING, false, false);
        when(paymentGatewayPort.createAccountLink(any(String.class), any(String.class), any(String.class)))
                .thenReturn(ONBOARDING_URL);

        String before = jdbcTemplate.queryForObject(
                "SELECT updated_at FROM vendor_stripe_accounts WHERE vendor_id = ?",
                String.class, VENDOR_ID);

        String url = refreshOnboardingLinkUseCase.execute(VENDOR_ID);

        assertEquals(ONBOARDING_URL, url);
        String after = jdbcTemplate.queryForObject(
                "SELECT updated_at FROM vendor_stripe_accounts WHERE vendor_id = ?",
                String.class, VENDOR_ID);
        assertEquals(before, after);
    }

    private void saveAccount(VendorAccountStatus status, boolean chargesEnabled, boolean payoutsEnabled) {
        vendorStripeAccountJpaRepository.save(VendorStripeAccountEntity.builder()
                .vendorId(VENDOR_ID)
                .stripeAccountId(STRIPE_ACCOUNT_ID)
                .accountStatus(status)
                .chargesEnabled(chargesEnabled)
                .payoutsEnabled(payoutsEnabled)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }
}