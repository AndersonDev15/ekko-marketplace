package com.ekko.order_service.domain.service;

import java.math.BigDecimal;

public record OrderTotals(
        BigDecimal subtotal,
        BigDecimal shippingCost,
        BigDecimal discount,
        BigDecimal total
) {
}