package com.ekko.payment_service.application.service;

import com.ekko.payment_service.config.AbstractPostgresIntegrationTest;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.model.PaymentStatus;
import com.ekko.payment_service.domain.model.VendorAllocation;
import com.ekko.payment_service.domain.port.in.ProcessTransfersUseCase;
import com.ekko.payment_service.domain.port.out.PaymentGatewayPort;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.infrastructure.persistence.entity.VendorAccountStatus;
import com.ekko.payment_service.infrastructure.persistence.entity.VendorStripeAccountEntity;
import com.ekko.payment_service.infrastructure.persistence.repository.VendorStripeAccountJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final UUID VENDOR_ONE = UUID.randomUUID();
    private static final UUID VENDOR_TWO = UUID.randomUUID();
    private static final UUID VENDOR_THREE = UUID.randomUUID();

    @Autowired
    private ProcessTransfersUseCase processTransfersUseCase;

    @Autowired
    private PaymentRepositoryPort paymentRepositoryPort;

    @Autowired
    private VendorStripeAccountJpaRepository vendorStripeAccountJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private PaymentGatewayPort paymentGatewayPort;

    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM payment_transfers");
        jdbcTemplate.execute("DELETE FROM payment_vendor_allocations");
        jdbcTemplate.execute("DELETE FROM payments");
        jdbcTemplate.execute("DELETE FROM vendor_stripe_accounts");
    }

    @Test
    void isolatesVendorFailureWhilePersistingSuccessfulTransfers() {
        Payment payment = saveSucceededPaymentWithThreeAllocations();
        saveVendorAccount(VENDOR_ONE, "acct_1");
        saveVendorAccount(VENDOR_TWO, "acct_2");
        when(paymentGatewayPort.createTransfer(any(BigDecimal.class), any(String.class), any(String.class),
                any(String.class)))
                .thenReturn("tr_1")
                .thenReturn("tr_2");

        processTransfersUseCase.execute(payment.getId());

        List<Map<String, Object>> transfers = jdbcTemplate.queryForList(
                "SELECT vendor_id, status, stripe_transfer_id, amount FROM payment_transfers WHERE payment_id = ?",
                payment.getId());
        assertEquals(3, transfers.size());

        Map<String, Object> transferOne = transferFor(transfers, VENDOR_ONE);
        assertEquals("SUCCEEDED", transferOne.get("status"));
        assertEquals("tr_1", transferOne.get("stripe_transfer_id"));
        assertEquals(0, new BigDecimal("45.00").compareTo((BigDecimal) transferOne.get("amount")));

        Map<String, Object> transferTwo = transferFor(transfers, VENDOR_TWO);
        assertEquals("SUCCEEDED", transferTwo.get("status"));
        assertEquals("tr_2", transferTwo.get("stripe_transfer_id"));
        assertEquals(0, new BigDecimal("36.00").compareTo((BigDecimal) transferTwo.get("amount")));

        Map<String, Object> transferThree = transferFor(transfers, VENDOR_THREE);
        assertEquals("FAILED", transferThree.get("status"));
        assertNull(transferThree.get("stripe_transfer_id"));
        assertEquals(0, new BigDecimal("27.00").compareTo((BigDecimal) transferThree.get("amount")));

        verify(paymentGatewayPort, times(2))
                .createTransfer(any(BigDecimal.class), any(String.class), any(String.class), any(String.class));
        verify(paymentGatewayPort).createTransfer(new BigDecimal("45.00"), "USD", "acct_1", payment.getId().toString());
        verify(paymentGatewayPort).createTransfer(new BigDecimal("36.00"), "USD", "acct_2", payment.getId().toString());
    }

    @Test
    void secondExecutionIsIdempotentAndDoesNotCallGatewayAgain() {
        Payment payment = saveSucceededPaymentWithThreeAllocations();
        saveVendorAccount(VENDOR_ONE, "acct_1");
        saveVendorAccount(VENDOR_TWO, "acct_2");
        when(paymentGatewayPort.createTransfer(any(BigDecimal.class), any(String.class), any(String.class),
                any(String.class)))
                .thenReturn("tr_1")
                .thenReturn("tr_2");

        processTransfersUseCase.execute(payment.getId());
        processTransfersUseCase.execute(payment.getId());

        List<Map<String, Object>> transfers = jdbcTemplate.queryForList(
                "SELECT vendor_id, status, stripe_transfer_id FROM payment_transfers WHERE payment_id = ?",
                payment.getId());
        assertEquals(3, transfers.size());
        verify(paymentGatewayPort, times(2))
                .createTransfer(any(BigDecimal.class), any(String.class), any(String.class), any(String.class));
    }

    private Payment saveSucceededPaymentWithThreeAllocations() {
        Payment payment = Payment.initiate(
                UUID.randomUUID(),
                null,
                "guest@example.com",
                new BigDecimal("120.00"),
                "USD");
        payment.attachPaymentIntent("pi_transfer_test", "cus_test_123");
        payment.markSucceeded("card", "4242", LocalDateTime.now());
        payment.addAllocations(List.of(
                new VendorAllocation(VENDOR_ONE, new BigDecimal("50.00"), new BigDecimal("5.00"),
                        new BigDecimal("45.00")),
                new VendorAllocation(VENDOR_TWO, new BigDecimal("40.00"), new BigDecimal("4.00"),
                        new BigDecimal("36.00")),
                new VendorAllocation(VENDOR_THREE, new BigDecimal("30.00"), new BigDecimal("3.00"),
                        new BigDecimal("27.00"))));
        return paymentRepositoryPort.save(payment);
    }

    private void saveVendorAccount(UUID vendorId, String stripeAccountId) {
        vendorStripeAccountJpaRepository.save(VendorStripeAccountEntity.builder()
                .vendorId(vendorId)
                .stripeAccountId(stripeAccountId)
                .accountStatus(VendorAccountStatus.ACTIVE)
                .chargesEnabled(true)
                .payoutsEnabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    private Map<String, Object> transferFor(List<Map<String, Object>> transfers, UUID vendorId) {
        return transfers.stream()
                .filter(t -> t.get("vendor_id").equals(vendorId))
                .findFirst()
                .orElseThrow();
    }
}