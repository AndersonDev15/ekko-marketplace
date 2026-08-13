package com.ekko.payment_service.domain.model;

import java.util.UUID;

public record VendorStripeAccount(
        UUID vendorId,
        String stripeAccountId,
        boolean chargesEnabled
) {
}
