package com.ekko.payment_service.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record VendorAllocation(
        UUID vendorId,
        BigDecimal grossAmount,
        BigDecimal applicationFeeAmount,
        BigDecimal netAmount
) {
}
