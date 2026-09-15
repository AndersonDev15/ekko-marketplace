package com.ekko.payment_service.infrastructure.persistence.adapter.in.web.controller;

import com.ekko.payment_service.domain.port.in.ProcessTransfersUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/payments/transfers")
@RequiredArgsConstructor
@Tag(name = "Payment Transfers", description = "Payment transfer processing endpoints")
@SecurityRequirement(name = "bearerAuth")
public class PaymentTransferController {

    private final ProcessTransfersUseCase processTransfersUseCase;

    @Operation(
            summary = "Retry payment transfers",
            description = "Retries the transfer process for a payment that previously failed. Initiates transfers to all vendor accounts associated with the payment."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Transfer retry initiated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT"),
            @ApiResponse(responseCode = "404", description = "Payment not found"),
            @ApiResponse(responseCode = "503", description = "Payment gateway unavailable")
    })
    @PostMapping("/{paymentId}/retry")
    public ResponseEntity<Void> retry(
            @Parameter(description = "Payment ID", required = true) @PathVariable UUID paymentId) {
        processTransfersUseCase.execute(paymentId);
        return ResponseEntity.noContent().build();
    }
}
