package com.ekko.payment_service.infrastructure.persistence.adapter.in.web.controller;

import com.ekko.payment_service.application.exception.PaymentAccessDeniedException;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.model.PaymentTransaction;
import com.ekko.payment_service.domain.port.in.GetPaymentByIdUseCase;
import com.ekko.payment_service.domain.port.in.GetPaymentByOrderIdUseCase;
import com.ekko.payment_service.domain.port.in.GetPaymentCheckoutStatusUseCase;
import com.ekko.payment_service.domain.port.in.GetTransactionsByPaymentIdUseCase;
import com.ekko.payment_service.infrastructure.persistence.adapter.in.web.dto.response.PaymentCheckoutStatusResponse;
import com.ekko.payment_service.infrastructure.persistence.adapter.in.web.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments Query", description = "Payment query endpoints for retrieving payment information")
@SecurityRequirement(name = "bearerAuth")
public class PaymentQueryController {

    private final GetPaymentByIdUseCase getPaymentByIdUseCase;
    private final GetPaymentByOrderIdUseCase getPaymentByOrderIdUseCase;
    private final GetTransactionsByPaymentIdUseCase getTransactionsByPaymentIdUseCase;
    private final GetPaymentCheckoutStatusUseCase getPaymentCheckoutStatusUseCase;

    @Operation(
            summary = "Get payment by ID",
            description = "Retrieves a payment by its ID. Authenticated users can access their own payments. " +
                    "Guest users must provide the guestEmail query parameter matching the payment's guest email."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment found",
                    content = @Content(schema = @Schema(implementation = Payment.class))),
            @ApiResponse(responseCode = "400", description = "Invalid guest email"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT"),
            @ApiResponse(responseCode = "403", description = "Access denied - invalid or missing credentials"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getPaymentById(
            @Parameter(description = "Payment ID", required = true) @PathVariable UUID paymentId,
            @Parameter(description = "Guest email for guest payments") @RequestParam(required = false) String guestEmail,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UUID keycloakId = jwt != null ? keycloakId(jwt) : null;
        if (keycloakId == null && (guestEmail == null || guestEmail.isBlank())) {
            throw new PaymentAccessDeniedException();
        }
        Payment payment = getPaymentByIdUseCase.execute(paymentId, keycloakId, guestEmail);
        return ResponseEntity.ok(payment);
    }

    @Operation(
            summary = "Get payment by order ID",
            description = "Retrieves a payment by its associated order ID. Authenticated users can access their own payments. " +
                    "Guest users must provide the guestEmail query parameter matching the payment's guest email."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment found"),
            @ApiResponse(responseCode = "400", description = "Invalid guest email"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT"),
            @ApiResponse(responseCode = "403", description = "Access denied - invalid or missing credentials"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<Payment> getPaymentByOrderId(
            @Parameter(description = "Order ID", required = true) @PathVariable UUID orderId,
            @Parameter(description = "Guest email for guest payments") @RequestParam(required = false) String guestEmail,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UUID keycloakId = jwt != null ? keycloakId(jwt) : null;
        if (keycloakId == null && (guestEmail == null || guestEmail.isBlank())) {
            throw new PaymentAccessDeniedException();
        }
        Payment payment = getPaymentByOrderIdUseCase.execute(orderId, keycloakId, guestEmail);
        return ResponseEntity.ok(payment);
    }

    @Operation(
            summary = "Get payment checkout status by order ID",
            description = "Retrieves the checkout status for a payment by order ID. Includes the Stripe client secret for pending payments. " +
                    "Authenticated users can access their own payments. Guest users must provide the guestEmail query parameter."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Checkout status retrieved",
                    content = @Content(schema = @Schema(implementation = PaymentCheckoutStatusResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid guest email"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT"),
            @ApiResponse(responseCode = "403", description = "Access denied - invalid or missing credentials"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/by-order/{orderId}/checkout-status")
    public ResponseEntity<PaymentCheckoutStatusResponse> getCheckoutStatus(
            @Parameter(description = "Order ID", required = true) @PathVariable UUID orderId,
            @Parameter(description = "Guest email for guest payments") @RequestParam(required = false) String guestEmail,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UUID keycloakId = jwt != null ? keycloakId(jwt) : null;
        if (keycloakId == null && (guestEmail == null || guestEmail.isBlank())) {
            throw new PaymentAccessDeniedException();
        }
        PaymentCheckoutStatusResponse response =
                getPaymentCheckoutStatusUseCase.execute(orderId, keycloakId, guestEmail);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get payment transactions by payment ID",
            description = "Retrieves all transactions associated with a payment. Authenticated users can access their own payments. " +
                    "Guest users must provide the guestEmail query parameter matching the payment's guest email."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid guest email"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT"),
            @ApiResponse(responseCode = "403", description = "Access denied - invalid or missing credentials"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/{paymentId}/transactions")
    public ResponseEntity<List<com.ekko.payment_service.domain.model.PaymentTransaction>> getTransactionsByPaymentId(
            @Parameter(description = "Payment ID", required = true) @PathVariable UUID paymentId,
            @Parameter(description = "Guest email for guest payments") @RequestParam(required = false) String guestEmail,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UUID keycloakId = jwt != null ? keycloakId(jwt) : null;
        if (keycloakId == null && (guestEmail == null || guestEmail.isBlank())) {
            throw new PaymentAccessDeniedException();
        }
        List<com.ekko.payment_service.domain.model.PaymentTransaction> transactions =
                getTransactionsByPaymentIdUseCase.execute(paymentId, keycloakId, guestEmail);
        return ResponseEntity.ok(transactions);
    }

    private UUID keycloakId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}