package com.ekko.order_service.domain.service;

import com.ekko.order_service.domain.model.OrderItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.ekko.order_service.builder.OrderTestDataBuilder.anOrderItem;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderTotalsCalculatorTest {

    private final OrderTotalsCalculator calculator = new OrderTotalsCalculator();

    @Test
    void calculateSumsEachItemPriceByQuantity() {
        OrderItem first = anOrderItem(2, new BigDecimal("100.00"));
        OrderItem second = anOrderItem(1, new BigDecimal("50.50"));

        OrderTotals totals = calculator.calculate(List.of(first, second));

        assertEquals(new BigDecimal("250.50"), totals.subtotal());
        assertEquals(BigDecimal.ZERO, totals.shippingCost());
        assertEquals(BigDecimal.ZERO, totals.discount());
        assertEquals(new BigDecimal("250.50"), totals.total());
    }

    @Test
    void calculateProducesZeroTotalsForEmptyItems() {
        OrderTotals totals = calculator.calculate(List.of());

        assertEquals(BigDecimal.ZERO, totals.subtotal());
        assertEquals(BigDecimal.ZERO, totals.shippingCost());
        assertEquals(BigDecimal.ZERO, totals.discount());
        assertEquals(BigDecimal.ZERO, totals.total());
    }

    @Test
    void calculateUsesSnapshotPriceNotQuantityScaledTwice() {
        OrderItem item = anOrderItem(3, new BigDecimal("10.00"));

        OrderTotals totals = calculator.calculate(List.of(item));

        assertEquals(new BigDecimal("30.00"), totals.subtotal());
        assertEquals(new BigDecimal("30.00"), totals.total());
    }
}