package com.ekko.order_service.application.service;

import com.ekko.order_service.domain.exception.InvalidOrderStatusTransitionException;
import com.ekko.order_service.application.exception.OrderNotFoundException;
import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.enums.OrderChangeSource;
import com.ekko.order_service.domain.enums.OrderStatus;
import com.ekko.order_service.domain.event.OrderStatusChangedEvent;
import com.ekko.order_service.domain.policy.OrderStatusTransitionPolicy;
import com.ekko.order_service.domain.port.in.UpdateOrderStatusUseCase;
import com.ekko.order_service.domain.port.out.OrderEventPublisherPort;
import com.ekko.order_service.domain.port.out.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderStatusService implements UpdateOrderStatusUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final OrderStatusTransitionPolicy orderStatusTransitionPolicy;
    private final OrderTransactionService orderTransactionService;
    private final OrderEventPublisherPort orderEventPublisherPort;

    @Override
    public Order execute(String orderNumber, OrderStatus newStatus) {
        Order order = findOrder(orderNumber);

        OrderStatus previousStatus = order.getStatus();
        applyTransition(order, newStatus);

        Order saved = orderTransactionService.changeStatus(
                order,
                OrderChangeSource.ADMIN,
                null,
                "Order status manually changed by admin");

        orderEventPublisherPort.publishOrderStatusChanged(toEvent(saved, previousStatus));

        return saved;
    }

    private void applyTransition(Order order, OrderStatus newStatus) {
        switch (newStatus) {
            case SHIPPED -> order.ship(orderStatusTransitionPolicy);
            case DELIVERED -> order.deliver(orderStatusTransitionPolicy);
            default -> throw new InvalidOrderStatusTransitionException(
                    order.getStatus(), newStatus);
        }
    }

    private Order findOrder(String orderNumber) {
        return orderRepositoryPort.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));
    }

    private OrderStatusChangedEvent toEvent(Order saved, OrderStatus previousStatus) {
        return new OrderStatusChangedEvent(
                saved.getId(),
                saved.getOrderNumber(),
                saved.getCustomerId(),
                saved.getGuestEmail(),
                previousStatus,
                saved.getStatus(),
                saved.getUpdatedAt());
    }
}