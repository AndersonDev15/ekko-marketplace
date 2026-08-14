package com.ekko.payment_service.domain.port.out;

import com.ekko.payment_service.domain.model.VendorStripeAccount;

import java.util.Optional;
import java.util.UUID;

public interface VendorStripeAccountRepositoryPort {

    Optional<VendorStripeAccount> findByVendorId(UUID vendorId);

    Optional<VendorStripeAccount> findByStripeAccountId(String stripeAccountId);

    VendorStripeAccount save(VendorStripeAccount account);
}