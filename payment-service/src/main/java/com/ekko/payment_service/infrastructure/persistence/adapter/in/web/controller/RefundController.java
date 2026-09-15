package com.ekko.payment_service.infrastructure.persistence.adapter.in.web.controller;

import com.ekko.payment_service.application.exception.PaymentAccessDeniedException;
import com.ekko.payment_service.domain.command.CreateRefundCommand;
import com.ekko.payment_service.domain.model.Refund;
import com.ekko.payment_service.domain.port.in.RequestRefundUseCase;
import com.ekko.payment_service.infrastructure.persistence.adapter.in.web.dto.request.CreateRefundRequest;
import com.ekko.payment_service.infrastructure.persistence.adapter.in.web.exception.ErrorResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Refunds", description = "Payment refund management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class RefundController {

    private final RequestRefundUseCase requestRefundUseCase;

    @Operation(
            summary = "Request a refund for a payment",
            description = "Creates a refund request for a payment. Authenticated users can refund their own payments. " +
                    "Guest users must provide the guestEmail query parameter matching the payment's guest email. " +
                    "Returns the created refund details."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Refund requested successfully",
                    content = @Content(schema = @Schema(implementation = Refund.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT"),
            @ApiResponse(responseCode = "403", description = "Access denied - invalid or missing credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Payment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Refund not allowed or amount exceeded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Payment gateway unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{paymentId}/refunds")
    public ResponseEntity<Refund> requestRefund(
            @Parameter(description = "Payment ID", required = true) @PathVariable UUID paymentId,
            @Parameter(description = "Guest email for guest payments") @RequestParam(required = false) String guestEmail,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateRefundRequest request) {
        UUID keycloakId = jwt != null ? keycloakId(jwt) : null;
        if (keycloakId == null && (guestEmail == null || guestEmail.isBlank())) {
            throw new PaymentAccessDeniedException();
        }

        CreateRefundCommand command = new CreateRefundCommand(
                paymentId,
                request.amount(),
                request.reason(),
                keycloakId,
                guestEmail,
                request.notes());

        Refund refund = requestRefundUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(refund);
    }

    private UUID keycloakId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}