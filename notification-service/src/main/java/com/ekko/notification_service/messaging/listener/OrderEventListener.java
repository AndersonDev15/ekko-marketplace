package com.ekko.notification_service.messaging.listener;

import com.ekko.notification_service.config.RabbitMQConfig;
import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.messaging.dto.NotificationEvent;
import com.ekko.notification_service.messaging.event.OrderCancelledEvent;
import com.ekko.notification_service.messaging.event.OrderConfirmedEvent;
import com.ekko.notification_service.messaging.event.OrderCreatedEvent;
import com.ekko.notification_service.messaging.event.OrderStatusChangedEvent;
import com.ekko.notification_service.service.NotificationOrchestratorService;
import com.ekko.notification_service.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private static final List<NotificationType> SELLER_CHANNELS =
            List.of(NotificationType.EMAIL, NotificationType.IN_APP);

    private final NotificationOrchestratorService orchestrator;
    private final UserLookupService userLookupService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void onOrderCreated(OrderCreatedEvent event) {
        orchestrator.process(new NotificationEvent(
                event.customerId(),
                event.customerEmail(),
                "ORDER_CREATED",
                customerChannels(event.customerId()),
                Map.of(
                        "orderNumber", event.orderNumber(),
                        "total", event.total(),
                        "itemCount", event.items().size()
                )
        ));

        event.items().stream()
                .map(OrderCreatedEvent.OrderItemPayload::sellerKeycloakId)
                .distinct()
                .forEach(sellerId -> orchestrator.process(new NotificationEvent(
                        sellerId,
                        userLookupService.resolveEmail(sellerId),
                        "ORDER_CREATED_SELLER",
                        SELLER_CHANNELS,
                        Map.of(
                                "orderNumber", event.orderNumber(),
                                "itemCount", event.items().stream()
                                        .filter(item -> item.sellerKeycloakId().equals(sellerId))
                                        .count()
                        )
                )));
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_CONFIRMED_QUEUE)
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        orchestrator.process(new NotificationEvent(
                event.customerId(),
                event.customerEmail(),
                "ORDER_CONFIRMED",
                customerChannels(event.customerId()),
                Map.of(
                        "orderNumber", event.orderNumber(),
                        "itemCount", event.items().size()
                )
        ));

        event.items().stream()
                .map(OrderConfirmedEvent.OrderItemConfirmed::sellerKeycloakId)
                .distinct()
                .forEach(sellerId -> orchestrator.process(new NotificationEvent(
                        sellerId,
                        userLookupService.resolveEmail(sellerId),
                        "ORDER_CONFIRMED_SELLER",
                        SELLER_CHANNELS,
                        Map.of(
                                "orderNumber", event.orderNumber(),
                                "itemCount", event.items().stream()
                                        .filter(item -> item.sellerKeycloakId().equals(sellerId))
                                        .count()
                        )
                )));
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_CANCELLED_QUEUE)
    public void onOrderCancelled(OrderCancelledEvent event) {
        orchestrator.process(new NotificationEvent(
                event.customerId(),
                event.customerEmail(),
                "ORDER_CANCELLED",
                customerChannels(event.customerId()),
                Map.of(
                        "orderNumber", event.orderNumber(),
                        "refundRequired", event.refundRequired(),
                        "previousStatus", event.previousStatus()
                )
        ));

        event.items().stream()
                .map(OrderCancelledEvent.OrderItemCancelled::sellerKeycloakId)
                .distinct()
                .forEach(sellerId -> orchestrator.process(new NotificationEvent(
                        sellerId,
                        userLookupService.resolveEmail(sellerId),
                        "ORDER_CANCELLED_SELLER",
                        SELLER_CHANNELS,
                        Map.of(
                                "orderNumber", event.orderNumber(),
                                "refundRequired", event.refundRequired()
                        )
                )));
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_STATUS_CHANGED_QUEUE)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        orchestrator.process(new NotificationEvent(
                event.customerId(),
                event.customerEmail(),
                "ORDER_STATUS_CHANGED",
                customerChannels(event.customerId()),
                Map.of(
                        "orderNumber", event.orderNumber(),
                        "previousStatus", event.previousStatus(),
                        "newStatus", event.newStatus()
                )
        ));

        for (UUID sellerId : event.sellerKeycloakIds()) {
            orchestrator.process(new NotificationEvent(
                    sellerId,
                    userLookupService.resolveEmail(sellerId),
                    "ORDER_STATUS_CHANGED_SELLER",
                    SELLER_CHANNELS,
                    Map.of(
                            "orderNumber", event.orderNumber(),
                            "previousStatus", event.previousStatus(),
                            "newStatus", event.newStatus()
                    )
            ));
        }
    }

    private List<NotificationType> customerChannels(UUID customerId) {
        return customerId != null
                ? List.of(NotificationType.EMAIL, NotificationType.IN_APP)
                : List.of(NotificationType.EMAIL);
    }
}