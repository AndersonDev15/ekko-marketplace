package com.ekko.notification_service.messaging.listener;

import com.ekko.notification_service.config.RabbitMQConfig;
import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.messaging.dto.NotificationEvent;
import com.ekko.notification_service.messaging.event.InventoryLowStockEvent;
import com.ekko.notification_service.messaging.event.ProductDeactivatedEvent;
import com.ekko.notification_service.messaging.event.ProductPublishedEvent;
import com.ekko.notification_service.messaging.event.ProductRejectedEvent;
import com.ekko.notification_service.service.NotificationOrchestratorService;
import com.ekko.notification_service.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProductEventListener {

    private static final List<NotificationType> PRODUCT_CHANNELS =
            List.of(NotificationType.EMAIL, NotificationType.IN_APP);

    private final NotificationOrchestratorService orchestrator;
    private final UserLookupService userLookupService;

    @RabbitListener(queues = RabbitMQConfig.PRODUCT_PUBLISHED_QUEUE)
    public void onProductPublished(ProductPublishedEvent event) {
        String email = userLookupService.resolveEmail(event.sellerKeycloakId());
        orchestrator.process(new NotificationEvent(
                event.sellerKeycloakId(),
                email,
                "PRODUCT_PUBLISHED",
                PRODUCT_CHANNELS,
                Map.of(
                        "productId", event.productId(),
                        "name", event.name(),
                        "category", event.category()
                )
        ));
    }

    @RabbitListener(queues = RabbitMQConfig.PRODUCT_REJECTED_QUEUE)
    public void onProductRejected(ProductRejectedEvent event) {
        String email = userLookupService.resolveEmail(event.sellerKeycloakId());
        orchestrator.process(new NotificationEvent(
                event.sellerKeycloakId(),
                email,
                "PRODUCT_REJECTED",
                PRODUCT_CHANNELS,
                Map.of(
                        "productId", event.productId(),
                        "name", event.name(),
                        "reason", event.reason()
                )
        ));
    }

    @RabbitListener(queues = RabbitMQConfig.PRODUCT_DEACTIVATED_QUEUE)
    public void onProductDeactivated(ProductDeactivatedEvent event) {
        String email = userLookupService.resolveEmail(event.sellerKeycloakId());
        orchestrator.process(new NotificationEvent(
                event.sellerKeycloakId(),
                email,
                "PRODUCT_DEACTIVATED",
                PRODUCT_CHANNELS,
                Map.of(
                        "productId", event.productId(),
                        "previousStatus", event.previousStatus()
                )
        ));
    }

    @RabbitListener(queues = RabbitMQConfig.INVENTORY_LOW_STOCK_QUEUE)
    public void onInventoryLowStock(InventoryLowStockEvent event) {
        String email = userLookupService.resolveEmail(event.sellerKeycloakId());
        orchestrator.process(new NotificationEvent(
                event.sellerKeycloakId(),
                email,
                "INVENTORY_LOW_STOCK",
                PRODUCT_CHANNELS,
                Map.of(
                        "productId", event.productId(),
                        "variantId", event.variantId(),
                        "currentStock", event.currentStock(),
                        "minimumStock", event.minimumStock()
                )
        ));
    }
}