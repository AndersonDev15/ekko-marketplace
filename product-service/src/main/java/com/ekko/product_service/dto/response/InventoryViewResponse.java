package com.ekko.product_service.dto.response;

import java.util.UUID;

public record InventoryViewResponse(
        UUID id,
        Long stockAvailable,
        Long stockReserved,
        Long stockMinimum,
        long availableForSale
) {
}