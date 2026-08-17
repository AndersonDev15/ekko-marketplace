package com.ekko.payment_service.application.service;

import com.ekko.payment_service.config.AbstractPostgresIntegrationTest;
import com.ekko.payment_service.domain.command.HandleAccountUpdatedCommand;
import com.ekko.payment_service.domain.port.in.HandleAccountUpdatedUseCase;
import com.ekko.payment_service.infrastructure.persistence.enums.VendorAccountStatus;
import com.ekko.payment_service.infrastructure.persistence.entity.VendorStripeAccountEntity;
import com.ekko.payment_service.infrastructure.persistence.repository.VendorStripeAccountJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class VendorAccountUpdateServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final UUID VENDOR_ID = UUID.randomUUID();
    private static final String STRIPE_ACCOUNT_ID = "acct_1";

    @Autowired
    private HandleAccountUpdatedUseCase handleAccountUpdatedUseCase;

    @Autowired
    private VendorStripeAccountJpaRepository vendorStripeAccountJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM vendor_stripe_accounts");
    }

    @Test
    void handleAccountUpdatedTransitionsPendingToActiveInDatabase() {
        saveAccount(VendorAccountStatus.PENDING, false, false);

        handleAccountUpdatedUseCase.execute(handleAccountUpdated(true, true, false, false));

        Map<String, Object> row = singleRow();
        assertEquals("ACTIVE", row.get("account_status"));
        assertEquals(true, row.get("charges_enabled"));
        assertEquals(true, row.get("payouts_enabled"));
        assertNotNull(row.get("updated_at"));
    }

    @Test
    void handleAccountUpdatedTransitionsToDisabledEvenWhenChargesAndPayoutsEnabled() {
        saveAccount(VendorAccountStatus.PENDING, false, false);

        handleAccountUpdatedUseCase.execute(handleAccountUpdated(true, true, true, true));

        Map<String, Object> row = singleRow();
        assertEquals("DISABLED", row.get("account_status"));
        assertEquals(true, row.get("charges_enabled"));
        assertEquals(true, row.get("payouts_enabled"));
    }

    private Map<String, Object> singleRow() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT account_status, charges_enabled, payouts_enabled, created_at, updated_at " +
                        "FROM vendor_stripe_accounts WHERE vendor_id = ?",
                VENDOR_ID);
        assertEquals(1, rows.size());
        return rows.get(0);
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