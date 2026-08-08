package com.ekko.order_service.domain.service;

import com.ekko.order_service.domain.model.OrderItem;

import java.math.BigDecimal;
import java.util.List;

public class OrderTotalsCalculator {

    public OrderTotals calculate(List<OrderItem> items) {
        BigDecimal shippingCost = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;

        BigDecimal subtotal = items.stream()
                .map(item -> item.priceSnapshot().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal total = subtotal.subtract(discount).add(shippingCost);

        return new OrderTotals(subtotal, shippingCost, discount, total);
    }
}