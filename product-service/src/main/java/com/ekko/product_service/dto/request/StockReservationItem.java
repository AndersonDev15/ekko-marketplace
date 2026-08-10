package com.ekko.product_service.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record StockReservationItem(
        @NotNull
        UUID variantId,

        @Positive
        long quantity
) {
}
