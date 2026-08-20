package com.ekko.product_service.messaging;

import com.ekko.product_service.messaging.dto.publish.InventoryLowStockEvent;
import com.ekko.product_service.messaging.dto.publish.ProductDeactivatedEvent;
import com.ekko.product_service.messaging.dto.publish.ProductPublishedEvent;
import com.ekko.product_service.messaging.dto.publish.ProductRejectedEvent;

public interface ProductEventPublisher {

    void publishProductPublished(ProductPublishedEvent event);

    void publishProductRejected(ProductRejectedEvent event);

    void publishProductDeactivated(ProductDeactivatedEvent event);

    void publishInventoryLowStock(InventoryLowStockEvent event);
}