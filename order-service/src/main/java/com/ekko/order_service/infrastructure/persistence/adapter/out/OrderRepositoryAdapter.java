package com.ekko.order_service.infrastructure.persistence.adapter.out;

import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.port.out.OrderRepositoryPort;
import com.ekko.order_service.infrastructure.persistence.entity.OrderEntity;
import com.ekko.order_service.infrastructure.persistence.mapper.OrderPersistenceMapper;
import com.ekko.order_service.infrastructure.persistence.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepositoryPort {

    private final OrderJpaRepository orderJpaRepository;
    private final OrderPersistenceMapper orderPersistenceMapper;

    @Override
    @Transactional
    public Order save(Order order) {
        OrderEntity entity = orderPersistenceMapper.toEntity(order);
        attachParentReference(entity);
        OrderEntity saved = orderJpaRepository.save(entity);
        return orderPersistenceMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(UUID id) {
        return orderJpaRepository.findById(id)
                .map(orderPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findByOrderNumber(String orderNumber) {
        return orderJpaRepository.findByOrderNumber(orderNumber)
                .map(orderPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Order> findAllByCustomerId(UUID customerId, Pageable pageable) {
        return orderJpaRepository.findAllByCustomerId(customerId, pageable)
                .map(orderPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Order> findAll(Pageable pageable) {
        return orderJpaRepository.findAll(pageable)
                .map(orderPersistenceMapper::toDomain);
    }

    private void attachParentReference(OrderEntity entity) {
        if (entity.getItems() != null) {
            entity.getItems().forEach(item -> item.setOrder(entity));
        }
        if (entity.getAddress() != null) {
            entity.getAddress().setOrder(entity);
        }
    }
}