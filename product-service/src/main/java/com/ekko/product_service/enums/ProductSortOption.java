package com.ekko.product_service.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Product sort options")
public enum ProductSortOption {
    PRICE_ASC,
    PRICE_DESC,
    RECENT,
    NAME
}