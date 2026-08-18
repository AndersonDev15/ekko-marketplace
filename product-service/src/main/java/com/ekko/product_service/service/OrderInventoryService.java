package com.ekko.product_service.service;

import com.ekko.product_service.messaging.dto.OrderCancelledEvent;
import com.ekko.product_service.messaging.dto.OrderConfirmedEvent;
import com.ekko.product_service.repository.OrderEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderInventoryService {

    public static final String EVENT_CONFIRMED = "CONFIRMED";
    public static final String EVENT_CANCELLED = "CANCELLED";

    private final OrderEventRepository orderEventRepository;
    private final InventoryService inventoryService;

    /**
     * Confirms stock for every item of a confirmed order. The order is marked as
     * processed atomically, so redeliveries do not adjust stock twice.
     */
    @Transactional
    public void confirmStock(OrderConfirmedEvent event) {
        if (orderEventRepository.insertIfAbsent(event.orderId(), EVENT_CONFIRMED) == 0) {
            return;
        }
        for (OrderConfirmedEvent.OrderItemConfirmed item : event.items()) {
            inventoryService.confirmStock(item.variantId(), item.quantity());
        }
    }

    /**
     * Releases stock for every item of a cancelled order. The order is marked as
     * processed atomically, so redeliveries do not release stock twice.
     */
    @Transactional
    public void releaseStock(OrderCancelledEvent event) {
        if (orderEventRepository.insertIfAbsent(event.orderId(), EVENT_CANCELLED) == 0) {
            return;
        }
        for (OrderCancelledEvent.OrderItemCancelled item : event.items()) {
            inventoryService.releaseStock(item.variantId(), item.quantity());
        }
    }
}