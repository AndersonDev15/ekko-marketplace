package com.ekko.order_service.infrastructure.persistence.adapter.in.web.controller;

import com.ekko.order_service.application.dto.CreateOrderRequest;
import com.ekko.order_service.application.dto.OrderDetailResponse;
import com.ekko.order_service.application.dto.OrderSummaryResponse;
import com.ekko.order_service.application.exception.OrderAccessDeniedException;
import com.ekko.order_service.application.mapper.OrderMapper;
import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.model.OrderAddress;
import com.ekko.order_service.domain.model.OrderDraft;
import com.ekko.order_service.domain.port.in.CancelOrderUseCase;
import com.ekko.order_service.domain.port.in.CreateOrderUseCase;
import com.ekko.order_service.domain.port.in.GetMyOrdersUseCase;
import com.ekko.order_service.domain.port.in.GetOrderByOrderNumberUseCase;
import com.ekko.order_service.infrastructure.persistence.adapter.in.web.dto.CancelOrderRequest;
import com.ekko.order_service.infrastructure.persistence.adapter.in.web.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Orders", description = "Customer-facing order management endpoints")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderByOrderNumberUseCase getOrderByOrderNumberUseCase;
    private final GetMyOrdersUseCase getMyOrdersUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final OrderMapper orderMapper;

    @Operation(
            summary = "Create a new order",
            description = "Creates a new order for authenticated customers or guest users. " +
                    "For authenticated users (JWT present), customerId and email are extracted from token. " +
                    "For guests, guestEmail from request body is used. " +
                    "Returns the created order with full details."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created successfully",
                    content = @Content(schema = @Schema(implementation = OrderDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Insufficient stock or order number generation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Stock service unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<OrderDetailResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {

        UUID customerId = jwt != null ? keycloakId(jwt) : null;
        String guestEmail = jwt != null ? null : request.guestEmail();
        String customerEmail = jwt != null ? jwt.getClaimAsString("email") : request.guestEmail();

        Order order = createOrderUseCase.execute(toDraft(request, customerId, guestEmail, customerEmail));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderMapper.toDetailResponse(order));
    }

    @Operation(
            summary = "Get order by order number",
            description = "Retrieves order details by order number. " +
                    "Authenticated users (JWT) can access their own orders. " +
                    "Guest users must provide the guestEmail query parameter matching the order's guest email. " +
                    "Returns 403 if access is denied."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order found",
                    content = @Content(schema = @Schema(implementation = OrderDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid guest email",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - invalid or missing credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderDetailResponse> getOrderByOrderNumber(
            @Parameter(description = "Order number", example = "ORD-20240115-ABC123") @PathVariable String orderNumber,
            @Parameter(description = "Guest email (required for guest orders)") @RequestParam(required = false) String guestEmail,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {

        UUID keycloakId = jwt != null ? keycloakId(jwt) : null;
        if (keycloakId == null && (guestEmail == null || guestEmail.isBlank())) {
            throw new OrderAccessDeniedException();
        }
        Order order = getOrderByOrderNumberUseCase.execute(orderNumber, keycloakId, guestEmail);
        return ResponseEntity.ok(orderMapper.toDetailResponse(order));
    }

    @Operation(
            summary = "Get authenticated user's orders",
            description = "Returns a paginated list of orders for the authenticated customer. " +
                    "Requires CUSTOMER role. Returns order summaries with basic information."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires CUSTOMER role")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/me")
    public ResponseEntity<Page<OrderSummaryResponse>> getMyOrders(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Pagination parameters") @PageableDefault(size = 20) Pageable pageable) {

        Page<Order> orders = getMyOrdersUseCase.execute(keycloakId(jwt), pageable);
        return ResponseEntity.ok(orders.map(orderMapper::toSummaryResponse));
    }

    @Operation(
            summary = "Cancel an order",
            description = "Cancels an order by order number. " +
                    "Authenticated users can cancel their own orders. " +
                    "Guest users must provide guestEmail in request body. " +
                    "Returns 403 if access is denied, 409 if cancellation is not allowed for the current order status."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order cancelled successfully",
                    content = @Content(schema = @Schema(implementation = OrderDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - invalid or missing credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Cancellation not allowed for current order status",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{orderNumber}/cancel")
    public ResponseEntity<OrderDetailResponse> cancelOrder(
            @Parameter(description = "Order number", example = "ORD-20240115-ABC123") @PathVariable String orderNumber,
            @RequestBody(required = false) CancelOrderRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {

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