package com.ekko.order_service.application.service;

import com.ekko.order_service.application.exception.OrderNotFoundException;
import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.event.OrderCancelledEvent;
import com.ekko.order_service.domain.enums.OrderChangeSource;
import com.ekko.order_service.domain.enums.OrderStatus;
import com.ekko.order_service.domain.policy.OrderCancellationPolicy;
import com.ekko.order_service.domain.policy.OrderOwnershipPolicy;
import com.ekko.order_service.domain.port.in.CancelOrderUseCase;
import com.ekko.order_service.domain.port.out.OrderEventPublisherPort;
import com.ekko.order_service.domain.port.out.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderCancellationService implements CancelOrderUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final OrderOwnershipPolicy orderOwnershipPolicy;
    private final OrderCancellationPolicy orderCancellationPolicy;
    private final OrderTransactionService orderTransactionService;
    private final OrderEventPublisherPort orderEventPublisherPort;

    @Override
    public Order execute(String orderNumber, UUID keycloakId, String guestEmail, boolean isAdmin) {
        Order order = findOrder(orderNumber);

        if (!isAdmin) {
            assertOwned(order, keycloakId, guestEmail);
        }

        orderCancellationPolicy.assertCancellable(order);

        OrderStatus previousStatus = order.getStatus();
        order.cancel();

        OrderChangeSource source = isAdmin
                ? OrderChangeSource.ADMIN
                : OrderChangeSource.CUSTOMER;
        UUID changedBy = isAdmin ? null : keycloakId;
        String notes = "Order cancelled" + (isAdmin ? " by admin" : "");

        Order saved = orderTransactionService.cancelOrder(order, source, changedBy, notes);

        orderEventPublisherPort.publishOrderCancelled(toEvent(saved, previousStatus));

        return saved;
    }

    public Order cancelBySystem(String orderNumber) {
        Order order = findOrder(orderNumber);

        orderCancellationPolicy.assertCancellable(order);

        OrderStatus previousStatus = order.getStatus();
        order.cancel();

        Order saved = orderTransactionService.cancelOrder(
                order,
                OrderChangeSource.SYSTEM,
                null,
                "Order cancelled after payment failure");

        orderEventPublisherPort.publishOrderCancelled(toEvent(saved, previousStatus));

        return saved;
    }

    private Order findOrder(String orderNumber) {
        return orderRepositoryPort.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));
    }

    private void assertOwned(Order order, UUID keycloakId, String guestEmail) {
        if (keycloakId != null) {
            orderOwnershipPolicy.assertCustomerOwns(order, keycloakId);
        } else if (guestEmail != null) {
            orderOwnershipPolicy.assertGuestOwns(order, guestEmail);
        }
    }

    private OrderCancelledEvent toEvent(Order saved, OrderStatus previousStatus) {
        return new OrderCancelledEvent(
                saved.getId(),
                saved.getOrderNumber(),
                saved.getCustomerId(),
                saved.getGuestEmail(),
                previousStatus,
                previousStatus == OrderStatus.CONFIRMED,
                saved.getItems().stream()
                        .map(item -> new OrderCancelledEvent.OrderItemCancelled(
                                item.id(),
                                item.productId(),
                                item.variantId(),
                                item.quantity()))
                        .toList(),
                saved.getUpdatedAt());
    }
}