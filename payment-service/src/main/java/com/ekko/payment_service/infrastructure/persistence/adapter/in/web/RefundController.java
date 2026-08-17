package com.ekko.payment_service.infrastructure.persistence.adapter.in.web;

import com.ekko.payment_service.domain.command.CreateRefundCommand;
import com.ekko.payment_service.domain.model.Refund;
import com.ekko.payment_service.domain.port.in.CreateRefundUseCase;
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
@RequestMapping("/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class RefundController {

    private final CreateRefundUseCase createRefundUseCase;

    @PostMapping("/{paymentId}/refunds")
    public ResponseEntity<RefundResponse> createRefund(
            @PathVariable UUID paymentId,
            @Valid @RequestBody CreateRefundRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID requestedBy = UUID.fromString(jwt.getSubject());
        Refund refund = createRefundUseCase.execute(new CreateRefundCommand(
                paymentId,
                request.amount(),
                request.reason(),
                request.notes(),
                requestedBy));

        return ResponseEntity.status(HttpStatus.CREATED).body(RefundResponse.from(refund));
    }
}