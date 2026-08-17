package com.ekko.order_service.application.service;

import com.ekko.order_service.application.exception.OrderNotFoundException;
import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.policy.OrderOwnershipPolicy;
import com.ekko.order_service.domain.port.in.GetAllOrdersUseCase;
import com.ekko.order_service.domain.port.in.GetMyOrdersUseCase;
import com.ekko.order_service.domain.port.in.GetOrderByOrderNumberForAdminUseCase;
import com.ekko.order_service.domain.port.in.GetOrderByOrderNumberUseCase;
import com.ekko.order_service.domain.port.out.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderQueryService implements
        GetOrderByOrderNumberUseCase,
        GetMyOrdersUseCase,
        GetAllOrdersUseCase,
        GetOrderByOrderNumberForAdminUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final OrderOwnershipPolicy orderOwnershipPolicy;

    @Override
    public Order execute(String orderNumber, UUID keycloakId, String guestEmail) {
        Order order = findOrder(orderNumber);
        if (keycloakId != null) {
            orderOwnershipPolicy.assertCustomerOwns(order, keycloakId);
        } else if (guestEmail != null) {
            orderOwnershipPolicy.assertGuestOwns(order, guestEmail);
        }
        return order;
    }

    @Override
    public Page<Order> execute(UUID keycloakId, Pageable pageable) {
        return orderRepositoryPort.findAllByCustomerId(keycloakId, pageable);
    }

    @Override
    public Page<Order> execute(Pageable pageable) {
        return orderRepositoryPort.findAll(pageable);
    }

    @Override
    public Order execute(String orderNumber) {
        return findOrder(orderNumber);
    }

    private Order findOrder(String orderNumber) {
        return orderRepositoryPort.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));
    }
}