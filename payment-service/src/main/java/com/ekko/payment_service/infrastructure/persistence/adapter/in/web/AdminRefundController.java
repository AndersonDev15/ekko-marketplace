package com.ekko.payment_service.infrastructure.persistence.adapter.in.web;

import com.ekko.payment_service.domain.command.CreateRefundCommand;
import com.ekko.payment_service.domain.model.Refund;
import com.ekko.payment_service.domain.port.in.ApproveRefundUseCase;
import com.ekko.payment_service.domain.port.in.RejectRefundUseCase;
import com.ekko.payment_service.infrastructure.persistence.adapter.in.web.dto.request.CreateRefundRequest;
import com.ekko.payment_service.infrastructure.persistence.adapter.in.web.dto.response.RefundResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/payments/refunds")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRefundController {

    private final ApproveRefundUseCase approveRefundUseCase;
    private final RejectRefundUseCase rejectRefundUseCase;

    @PostMapping("/{refundId}/approve")
    public ResponseEntity<Refund> approveRefund(@PathVariable UUID refundId) {
        return ResponseEntity.ok(approveRefundUseCase.execute(refundId));
    }

    @PostMapping("/{refundId}/reject")
    public ResponseEntity<Refund> rejectRefund(@PathVariable UUID refundId) {
        return ResponseEntity.ok(rejectRefundUseCase.execute(refundId));
    }
}