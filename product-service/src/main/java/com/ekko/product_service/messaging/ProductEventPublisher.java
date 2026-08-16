package com.ekko.product_service.messaging;

import com.ekko.product_service.messaging.dto.InventoryLowStockEvent;
import com.ekko.product_service.messaging.dto.ProductDeactivatedEvent;
import com.ekko.product_service.messaging.dto.ProductPublishedEvent;
import com.ekko.product_service.messaging.dto.ProductRejectedEvent;

public interface ProductEventPublisher {

    void publishProductPublished(ProductPublishedEvent event);

    void publishProductRejected(ProductRejectedEvent event);

    void publishProductDeactivated(ProductDeactivatedEvent event);

    void publishInventoryLowStock(InventoryLowStockEvent event);
}