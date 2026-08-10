package com.ekko.order_service.domain.port.out;

import com.ekko.order_service.domain.model.ProductVariant;
import com.ekko.order_service.domain.model.StockItem;

import java.util.List;
import java.util.UUID;

public interface ProductServicePort {

    List<ProductVariant> getVariantsInfo(List<UUID> variantIds);

    void reserveStock(List<StockItem> items);
}