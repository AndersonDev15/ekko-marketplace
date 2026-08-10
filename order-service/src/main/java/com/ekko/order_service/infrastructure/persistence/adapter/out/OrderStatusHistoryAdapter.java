package com.ekko.order_service.infrastructure.persistence.adapter.out;

import com.ekko.order_service.domain.exception.OrderNotFoundException;
import com.ekko.order_service.domain.model.OrderChangeSource;
import com.ekko.order_service.domain.model.OrderStatus;
import com.ekko.order_service.domain.port.out.OrderStatusHistoryPort;
import com.ekko.order_service.infrastructure.persistence.entity.ChangedByType;
import com.ekko.order_service.infrastructure.persistence.entity.OrderEntity;
import com.ekko.order_service.infrastructure.persistence.entity.OrderStatusHistoryEntity;
import com.ekko.order_service.infrastructure.persistence.repository.OrderJpaRepository;
import com.ekko.order_service.infrastructure.persistence.repository.OrderStatusHistoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderStatusHistoryAdapter implements OrderStatusHistoryPort {

    private final OrderStatusHistoryJpaRepository orderStatusHistoryJpaRepository;
    private final OrderJpaRepository orderJpaRepository;

    @Override
    @Transactional
    public void recordStatusChange(
            UUID orderId,
            OrderStatus newStatus,
            OrderChangeSource source,
            UUID changedBy,
            String notes) {

        OrderEntity order = orderJpaRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId.toString()));

        OrderStatusHistoryEntity history = OrderStatusHistoryEntity.builder()
                .order(order)
                .status(newStatus)
                .changedBy(changedBy)
                .changedByType(ChangedByType.valueOf(source.name()))
                .notes(notes)
                .createdAt(LocalDateTime.now())
                .build();

        orderStatusHistoryJpaRepository.save(history);
    }
}