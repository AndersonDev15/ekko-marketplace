package com.ekko.payment_service.infrastructure.persistence.adapter.in.web.controller;

import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.model.PaymentTransaction;
import com.ekko.payment_service.domain.port.in.GetPaymentByIdForAdminUseCase;
import com.ekko.payment_service.domain.port.in.GetPaymentByOrderIdForAdminUseCase;
import com.ekko.payment_service.domain.port.in.GetTransactionsByPaymentIdForAdminUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Payments", description = "Administrative payment query endpoints (requires ADMIN role)")
@SecurityRequirement(name = "bearerAuth")
public class AdminPaymentController {

    private final GetPaymentByIdForAdminUseCase getPaymentByIdForAdminUseCase;
    private final GetPaymentByOrderIdForAdminUseCase getPaymentByOrderIdForAdminUseCase;
    private final GetTransactionsByPaymentIdForAdminUseCase getTransactionsByPaymentIdForAdminUseCase;

    @Operation(
            summary = "Get payment by ID (admin)",
            description = "Retrieves a payment by its ID without ownership checks. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/{paymentId}")
    public ResponseEntity<com.ekko.payment_service.domain.model.Payment> getPaymentById(
            @Parameter(description = "Payment ID", required = true) @PathVariable UUID paymentId) {
        com.ekko.payment_service.domain.model.Payment payment = getPaymentByIdForAdminUseCase.execute(paymentId);
        return ResponseEntity.ok(payment);
    }

    @Operation(
            summary = "Get payment by order ID (admin)",
            description = "Retrieves a payment by its associated order ID without ownership checks. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<com.ekko.payment_service.domain.model.Payment> getPaymentByOrderId(
            @Parameter(description = "Order ID", required = true) @PathVariable UUID orderId) {
        com.ekko.payment_service.domain.model.Payment payment = getPaymentByOrderIdForAdminUseCase.execute(orderId);
        return ResponseEntity.ok(payment);
    }

    @Operation(
            summary = "Get payment transactions by payment ID (admin)",
            description = "Retrieves all transactions associated with a payment without ownership checks. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/{paymentId}/transactions")
    public ResponseEntity<List<com.ekko.payment_service.domain.model.PaymentTransaction>> getTransactionsByPaymentId(
            @Parameter(description = "Payment ID", required = true) @PathVariable UUID paymentId) {
        List<com.ekko.payment_service.domain.model.PaymentTransaction> transactions = getTransactionsByPaymentIdForAdminUseCase.execute(paymentId);
        return ResponseEntity.ok(transactions);
    }
}