package com.ekko.payment_service.domain.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record InitiatePaymentCommand(
        UUID orderId,
        UUID customerId,
        String guestEmail,
        String customerEmail,
        BigDecimal amount,
        String currency,
        List<VendorGrossAmount> vendorGrossAmounts
) {

    public record VendorGrossAmount(UUID vendorId, BigDecimal grossAmount) {
    }
}
