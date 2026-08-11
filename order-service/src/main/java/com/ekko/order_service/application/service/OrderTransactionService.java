package com.ekko.order_service.application.service;

import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.model.OrderChangeSource;
import com.ekko.order_service.domain.model.OrderStatus;
import com.ekko.order_service.domain.port.out.OrderRepositoryPort;
import com.ekko.order_service.domain.port.out.OrderStatusHistoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderTransactionService {

    private final OrderRepositoryPort orderRepositoryPort;
    private final OrderStatusHistoryPort orderStatusHistoryPort;

    @Transactional
    public Order commitOrder(Order order) {
        Order saved = orderRepositoryPort.save(order);

        orderStatusHistoryPort.recordStatusChange(
                saved.getId(),
                OrderStatus.PENDING,
                OrderChangeSource.SYSTEM,
                null,
                "Order created");

        return saved;
    }

    @Transactional
    public Order cancelOrder(Order order, OrderChangeSource source, UUID changedBy, String notes) {
        Order saved = orderRepositoryPort.save(order);

        orderStatusHistoryPort.recordStatusChange(
                saved.getId(),
                OrderStatus.CANCELLED,
                source,
                changedBy,
                notes);

        return saved;
    }

    @Transactional
    public Order changeStatus(Order order, OrderChangeSource source, UUID changedBy, String notes) {
        Order saved = orderRepositoryPort.save(order);

        orderStatusHistoryPort.recordStatusChange(
                saved.getId(),
                saved.getStatus(),
                source,
                changedBy,
                notes);

        return saved;
    }
}