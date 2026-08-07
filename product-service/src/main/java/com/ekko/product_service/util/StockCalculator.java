package com.ekko.product_service.util;

import com.ekko.product_service.entity.Inventory;

public final class StockCalculator {

    private StockCalculator() {
    }

    public static long getAvailableForSale(Inventory inventory) {
        return inventory.getStockAvailable() - inventory.getStockReserved();
    }

    public static boolean isLowStock(Inventory inventory) {
        return getAvailableForSale(inventory) <= inventory.getStockMinimum();
    }
}