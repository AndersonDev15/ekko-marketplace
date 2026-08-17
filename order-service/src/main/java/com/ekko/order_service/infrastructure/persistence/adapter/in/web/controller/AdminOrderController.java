package com.ekko.order_service.infrastructure.persistence.adapter.in.web.controller;

import com.ekko.order_service.application.dto.OrderDetailResponse;
import com.ekko.order_service.application.dto.OrderSummaryResponse;
import com.ekko.order_service.application.mapper.OrderMapper;
import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.port.in.CancelOrderUseCase;
import com.ekko.order_service.domain.port.in.GetAllOrdersUseCase;
import com.ekko.order_service.domain.port.in.GetOrderByOrderNumberForAdminUseCase;
import com.ekko.order_service.domain.port.in.UpdateOrderStatusUseCase;
import com.ekko.order_service.infrastructure.persistence.adapter.in.web.dto.UpdateOrderStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminOrderController {

    private final GetAllOrdersUseCase getAllOrdersUseCase;
    private final GetOrderByOrderNumberForAdminUseCase getOrderByOrderNumberForAdminUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private final OrderMapper orderMapper;


    @GetMapping
    public ResponseEntity<Page<OrderSummaryResponse>> getAllOrders(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Order> orders = getAllOrdersUseCase.execute(pageable);
        return ResponseEntity.ok(orders.map(orderMapper::toSummaryResponse));
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderDetailResponse> getOrderByOrderNumber(
            @PathVariable String orderNumber) {
        Order order = getOrderByOrderNumberForAdminUseCase.execute(orderNumber);
        return ResponseEntity.ok(orderMapper.toDetailResponse(order));
    }

    @PostMapping("/{orderNumber}/cancel")
    public ResponseEntity<OrderDetailResponse> cancelOrder(
            @PathVariable String orderNumber) {
        Order order = cancelOrderUseCase.execute(orderNumber, null, null, true);
        return ResponseEntity.ok(orderMapper.toDetailResponse(order));
    }

    @PatchMapping("/{orderNumber}/status")
    public ResponseEntity<OrderDetailResponse> updateOrderStatus(
            @PathVariable String orderNumber,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        Order order = updateOrderStatusUseCase.execute(orderNumber, request.newStatus());
        return ResponseEntity.ok(orderMapper.toDetailResponse(order));
    }
}