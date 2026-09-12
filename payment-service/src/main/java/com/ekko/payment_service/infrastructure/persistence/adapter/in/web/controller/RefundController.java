package com.ekko.payment_service.infrastructure.persistence.adapter.in.web.controller;

import com.ekko.payment_service.application.exception.PaymentAccessDeniedException;
import com.ekko.payment_service.domain.command.CreateRefundCommand;
import com.ekko.payment_service.domain.model.Refund;
import com.ekko.payment_service.domain.port.in.RequestRefundUseCase;
import com.ekko.payment_service.infrastructure.persistence.adapter.in.web.dto.request.CreateRefundRequest;
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
public class RefundController {

    private final RequestRefundUseCase requestRefundUseCase;

    @PostMapping("/{paymentId}/refunds")
    public ResponseEntity<Refund> requestRefund(
            @PathVariable UUID paymentId,
            @RequestParam(required = false) String guestEmail,
            @AuthenticationPrincipal Jwt jwt,
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