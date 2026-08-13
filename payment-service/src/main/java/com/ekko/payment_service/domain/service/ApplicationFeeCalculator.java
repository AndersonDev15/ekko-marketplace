package com.ekko.payment_service.domain.service;

import com.ekko.payment_service.domain.model.VendorAllocation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;


public class ApplicationFeeCalculator {

    private static final BigDecimal APPLICATION_FEE_RATE = new BigDecimal("0.10");

    public VendorAllocation calculate(UUID vendorId, BigDecimal grossAmount) {
        BigDecimal applicationFeeAmount = grossAmount
                .multiply(APPLICATION_FEE_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal netAmount = grossAmount.subtract(applicationFeeAmount);
        return new VendorAllocation(vendorId, grossAmount, applicationFeeAmount, netAmount);
    }
}
