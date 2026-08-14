package com.ekko.payment_service.domain.port.in;

import com.ekko.payment_service.domain.model.VendorStripeAccount;

import java.util.UUID;

public interface GetVendorAccountUseCase {

    VendorStripeAccount execute(UUID vendorId);
}