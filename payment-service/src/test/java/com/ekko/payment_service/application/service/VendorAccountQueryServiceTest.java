package com.ekko.payment_service.application.service;

import com.ekko.payment_service.application.exception.VendorAccountNotFoundException;
import com.ekko.payment_service.domain.enums.VendorAccountStatus;
import com.ekko.payment_service.domain.model.VendorStripeAccount;
import com.ekko.payment_service.domain.port.out.VendorStripeAccountRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorAccountQueryServiceTest {

    private static final UUID VENDOR_ID = UUID.randomUUID();

    @Mock
    private VendorStripeAccountRepositoryPort vendorStripeAccountRepositoryPort;

    private VendorAccountQueryService service;

    @BeforeEach
    void setUp() {
        service = new VendorAccountQueryService(vendorStripeAccountRepositoryPort);
    }

    @Test
    void getVendorAccountReturnsTheAccount() {
        VendorStripeAccount account = VendorStripeAccount.restore(
                UUID.randomUUID(),
                VENDOR_ID,
                "acct_1",
                VendorAccountStatus.ACTIVE,
                true,
                true,
                LocalDateTime.now(),
                LocalDateTime.now());
        when(vendorStripeAccountRepositoryPort.findByVendorId(VENDOR_ID))
                .thenReturn(Optional.of(account));

        VendorStripeAccount result = service.execute(VENDOR_ID);

        assertEquals(account, result);
    }

    @Test
    void getVendorAccountThrowsWhenNotFound() {
        when(vendorStripeAccountRepositoryPort.findByVendorId(VENDOR_ID)).thenReturn(Optional.empty());

        assertThrows(VendorAccountNotFoundException.class, () -> service.execute(VENDOR_ID));
    }
}