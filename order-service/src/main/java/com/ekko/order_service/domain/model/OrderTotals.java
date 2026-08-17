package com.ekko.order_service.domain.model;

import java.math.BigDecimal;

public record OrderTotals(
        BigDecimal subtotal,
        BigDecimal shippingCost,
        BigDecimal discount,
        BigDecimal total
) {
}