package com.ekko.payment_service.infrastructure.persistence.adapter.in.web.controller;

import com.ekko.payment_service.domain.model.Refund;
import com.ekko.payment_service.domain.port.in.ApproveRefundUseCase;
import com.ekko.payment_service.domain.port.in.RejectRefundUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/payments/refunds")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Refunds", description = "Administrative refund management endpoints (requires ADMIN role)")
@SecurityRequirement(name = "bearerAuth")
public class AdminRefundController {

    private final ApproveRefundUseCase approveRefundUseCase;
    private final RejectRefundUseCase rejectRefundUseCase;

    @Operation(
            summary = "Approve a refund",
            description = "Approves a pending refund request. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Refund approved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Refund not found"),
            @ApiResponse(responseCode = "409", description = "Refund cannot be approved in current state")
    })
    @PostMapping("/{refundId}/approve")
    public ResponseEntity<com.ekko.payment_service.domain.model.Refund> approveRefund(
            @Parameter(description = "Refund ID", required = true) @PathVariable UUID refundId) {
        return ResponseEntity.ok(approveRefundUseCase.execute(refundId));
    }

    @Operation(
            summary = "Reject a refund",
            description = "Rejects a pending refund request. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Refund rejected successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Refund not found"),
            @ApiResponse(responseCode = "409", description = "Refund cannot be rejected in current state")
    })
    @PostMapping("/{refundId}/reject")
    public ResponseEntity<com.ekko.payment_service.domain.model.Refund> rejectRefund(
            @Parameter(description = "Refund ID", required = true) @PathVariable UUID refundId) {
        return ResponseEntity.ok(rejectRefundUseCase.execute(refundId));
    }
}