package com.ekko.payment_service.domain.service;

import com.ekko.payment_service.domain.model.VendorAllocation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationFeeCalculatorTest {

    private final ApplicationFeeCalculator calculator = new ApplicationFeeCalculator();

    @Test
    void calculatesTenPercentForWholeAmount() {
        VendorAllocation allocation = calculator.calculate(
                UUID.randomUUID(),
                new BigDecimal("100.00"));

        assertEquals(0, new BigDecimal("10.00").compareTo(allocation.applicationFeeAmount()));
        assertEquals(0, new BigDecimal("90.00").compareTo(allocation.netAmount()));
    }

    @Test
    void roundsDecimalsHalfUp() {
        VendorAllocation allocation = calculator.calculate(
                UUID.randomUUID(),
                new BigDecimal("33.33"));

        assertEquals(0, new BigDecimal("3.33").compareTo(allocation.applicationFeeAmount()));
        assertEquals(0, new BigDecimal("30.00").compareTo(allocation.netAmount()));
    }

    @Test
    void roundsHalfUpAtBoundary() {
        VendorAllocation allocation = calculator.calculate(
                UUID.randomUUID(),
                new BigDecimal("33.35"));

        assertEquals(0, new BigDecimal("3.34").compareTo(allocation.applicationFeeAmount()));
        assertEquals(0, new BigDecimal("30.01").compareTo(allocation.netAmount()));
    }
}