package com.ekko.order_service.infrastructure.web;

import com.ekko.order_service.application.dto.CreateOrderRequest;
import com.ekko.order_service.application.dto.OrderDetailResponse;
import com.ekko.order_service.application.dto.OrderSummaryResponse;
import com.ekko.order_service.application.mapper.OrderMapper;
import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.model.OrderAddress;
import com.ekko.order_service.domain.model.OrderDraft;
import com.ekko.order_service.domain.exception.OrderAccessDeniedException;
import com.ekko.order_service.domain.port.in.CancelOrderUseCase;
import com.ekko.order_service.domain.port.in.CreateOrderUseCase;
import com.ekko.order_service.domain.port.in.GetMyOrdersUseCase;
import com.ekko.order_service.domain.port.in.GetOrderByOrderNumberUseCase;
import com.ekko.order_service.infrastructure.web.dto.CancelOrderRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderByOrderNumberUseCase getOrderByOrderNumberUseCase;
    private final GetMyOrdersUseCase getMyOrdersUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final OrderMapper orderMapper;

    @PostMapping
    public ResponseEntity<OrderDetailResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID customerId = jwt != null ? keycloakId(jwt) : null;
        String guestEmail = jwt != null ? null : request.guestEmail();
        String customerEmail = jwt != null ? jwt.getClaimAsString("email") : request.guestEmail();

        Order order = createOrderUseCase.execute(toDraft(request, customerId, guestEmail, customerEmail));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderMapper.toDetailResponse(order));
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderDetailResponse> getOrderByOrderNumber(
            @PathVariable String orderNumber,
            @RequestParam(required = false) String guestEmail,
            @AuthenticationPrincipal Jwt jwt) {

        UUID keycloakId = jwt != null ? keycloakId(jwt) : null;
        if (keycloakId == null && (guestEmail == null || guestEmail.isBlank())) {
            throw new OrderAccessDeniedException();
        }
        Order order = getOrderByOrderNumberUseCase.execute(orderNumber, keycloakId, guestEmail);
        return ResponseEntity.ok(orderMapper.toDetailResponse(order));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<OrderSummaryResponse>> getMyOrders(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<Order> orders = getMyOrdersUseCase.execute(keycloakId(jwt), pageable);
        return ResponseEntity.ok(orders.map(orderMapper::toSummaryResponse));
    }

    @PostMapping("/{orderNumber}/cancel")
    public ResponseEntity<OrderDetailResponse> cancelOrder(
            @PathVariable String orderNumber,
            @RequestBody(required = false) CancelOrderRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID keycloakId = jwt != null ? keycloakId(jwt) : null;
        String guestEmail = jwt != null
                ? null
                : (request != null ? request.guestEmail() : null);
        if (keycloakId == null && (guestEmail == null || guestEmail.isBlank())) {
            throw new OrderAccessDeniedException();
        }

        Order order = cancelOrderUseCase.execute(orderNumber, keycloakId, guestEmail, false);
        return ResponseEntity.ok(orderMapper.toDetailResponse(order));
    }

    private OrderDraft toDraft(CreateOrderRequest request, UUID customerId, String guestEmail, String customerEmail) {
        CreateOrderRequest.AddressRequest addressRequest = request.shippingAddress();
        OrderAddress address = new OrderAddress(
                null,
                addressRequest.fullName(),
                addressRequest.phone(),
                addressRequest.addressLine(),
                addressRequest.city(),
                addressRequest.state(),
                addressRequest.country(),
                addressRequest.postalCode());

        List<OrderDraft.OrderItemDraft> items = request.items().stream()
                .map(item -> new OrderDraft.OrderItemDraft(item.variantId(), item.quantity()))
                .toList();

        return new OrderDraft(customerId, guestEmail, customerEmail, address, items, request.notes());
    }

    private UUID keycloakId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}