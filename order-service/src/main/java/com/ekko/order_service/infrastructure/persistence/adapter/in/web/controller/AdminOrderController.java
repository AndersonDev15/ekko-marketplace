package com.ekko.order_service.infrastructure.persistence.adapter.in.web.controller;

import com.ekko.order_service.application.dto.OrderDetailResponse;
import com.ekko.order_service.application.dto.OrderSummaryResponse;
import com.ekko.order_service.application.mapper.OrderMapper;
import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.port.in.CancelOrderUseCase;
import com.ekko.order_service.domain.port.in.GetAllOrdersUseCase;
import com.ekko.order_service.domain.port.in.GetOrderByOrderNumberForAdminUseCase;
import com.ekko.order_service.domain.port.in.UpdateOrderStatusUseCase;
import com.ekko.order_service.domain.enums.OrderStatus;
import com.ekko.order_service.infrastructure.persistence.adapter.in.web.dto.UpdateOrderStatusRequest;
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
@Tag(name = "Admin Orders", description = "Administrative order management endpoints (requires ADMIN role)")
@SecurityRequirement(name = "bearerAuth")
public class AdminOrderController {

    private final GetAllOrdersUseCase getAllOrdersUseCase;
    private final GetOrderByOrderNumberForAdminUseCase getOrderByOrderNumberForAdminUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private final OrderMapper orderMapper;


    @Operation(
            summary = "Get all orders (admin)",
            description = "Returns a paginated list of all orders in the system. " +
                    "Requires ADMIN role. Returns order summaries with basic information."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    @GetMapping
    public ResponseEntity<Page<OrderSummaryResponse>> getAllOrders(
            @Parameter(description = "Pagination parameters") @PageableDefault(size = 20) Pageable pageable) {
        Page<Order> orders = getAllOrdersUseCase.execute(pageable);
        return ResponseEntity.ok(orders.map(orderMapper::toSummaryResponse));
    }

    @Operation(
            summary = "Get order by order number (admin)",
            description = "Retrieves full order details by order number for admin users. " +
                    "Requires ADMIN role. No ownership checks are performed."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order found",
                    content = @Content(schema = @Schema(implementation = OrderDetailResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderDetailResponse> getOrderByOrderNumber(
            @Parameter(description = "Order number", example = "ORD-20240115-ABC123") @PathVariable String orderNumber) {
        Order order = getOrderByOrderNumberForAdminUseCase.execute(orderNumber);
        return ResponseEntity.ok(orderMapper.toDetailResponse(order));
    }

    @Operation(
            summary = "Cancel an order (admin)",
            description = "Cancels an order by order number as admin. " +
                    "Requires ADMIN role. Admin cancellation bypasses ownership checks. " +
                    "Returns 409 if cancellation is not allowed for the current order status."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order cancelled successfully",
                    content = @Content(schema = @Schema(implementation = OrderDetailResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Cancellation not allowed for current order status",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{orderNumber}/cancel")
    public ResponseEntity<OrderDetailResponse> cancelOrder(
            @Parameter(description = "Order number", example = "ORD-20240115-ABC123") @PathVariable String orderNumber) {
        Order order = cancelOrderUseCase.execute(orderNumber, null, null, true);
        return ResponseEntity.ok(orderMapper.toDetailResponse(order));
    }

    @Operation(
            summary = "Update order status (admin)",
            description = "Updates the status of an order. " +
                    "Requires ADMIN role. Valid status transitions are enforced: " +
                    "PENDING -> CONFIRMED, CONFIRMED -> SHIPPED, SHIPPED -> DELIVERED, " +
                    "any status -> CANCELLED (subject to cancellation policy). " +
                    "Returns 409 if the transition is invalid."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order status updated successfully",
                    content = @Content(schema = @Schema(implementation = OrderDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error - missing or invalid status",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Invalid status transition",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{orderNumber}/status")
    public ResponseEntity<OrderDetailResponse> updateOrderStatus(
            @Parameter(description = "Order number", example = "ORD-20240115-ABC123") @PathVariable String orderNumber,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        Order order = updateOrderStatusUseCase.execute(orderNumber, request.newStatus());
        return ResponseEntity.ok(orderMapper.toDetailResponse(order));
    }
}