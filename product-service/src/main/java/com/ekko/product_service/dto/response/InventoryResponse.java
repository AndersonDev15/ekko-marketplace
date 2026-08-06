package com.ekko.product_service.dto.response;

import java.util.UUID;

public record InventoryResponse(
        UUID id,
        Long stockAvailable,
        Long stockReserved,
        Long stockMinimum
) {
}