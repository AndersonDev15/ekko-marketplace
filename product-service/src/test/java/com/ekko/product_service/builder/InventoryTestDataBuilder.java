package com.ekko.product_service.builder;

import com.ekko.product_service.entity.Inventory;
import com.ekko.product_service.entity.ProductVariant;

import java.time.LocalDateTime;
import java.util.UUID;

public class InventoryTestDataBuilder {

    private UUID id = UUID.randomUUID();
    private ProductVariant variant = null;
    private Long stockAvailable = 10L;
    private Long stockReserved = 0L;
    private Long stockMinimum = 3L;
    private LocalDateTime updatedAt = LocalDateTime.of(2025, 1, 1, 0, 0);

    private InventoryTestDataBuilder() {
    }

    public static InventoryTestDataBuilder anInventory() {
        return new InventoryTestDataBuilder();
    }

    public InventoryTestDataBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public InventoryTestDataBuilder withVariant(ProductVariant variant) {
        this.variant = variant;
        return this;
    }

    public InventoryTestDataBuilder withStockAvailable(Long stockAvailable) {
        this.stockAvailable = stockAvailable;
        return this;
    }

    public InventoryTestDataBuilder withStockReserved(Long stockReserved) {
        this.stockReserved = stockReserved;
        return this;
    }

    public InventoryTestDataBuilder withStockMinimum(Long stockMinimum) {
        this.stockMinimum = stockMinimum;
        return this;
    }

    public Inventory build() {
        Inventory inventory = new Inventory();
        inventory.setId(id);
        inventory.setVariant(variant);
        inventory.setStockAvailable(stockAvailable);
        inventory.setStockReserved(stockReserved);
        inventory.setStockMinimum(stockMinimum);
        inventory.setUpdatedAt(updatedAt);
        return inventory;
    }
}