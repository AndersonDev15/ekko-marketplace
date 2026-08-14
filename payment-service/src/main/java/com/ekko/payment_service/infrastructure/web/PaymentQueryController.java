package com.ekko.payment_service.infrastructure.web;

import com.ekko.payment_service.domain.exception.PaymentAccessDeniedException;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.model.PaymentTransaction;
import com.ekko.payment_service.domain.port.in.GetPaymentByIdUseCase;
import com.ekko.payment_service.domain.port.in.GetPaymentByOrderIdUseCase;
import com.ekko.payment_service.domain.port.in.GetTransactionsByPaymentIdUseCase;
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
public class PaymentQueryController {

    private final GetPaymentByIdUseCase getPaymentByIdUseCase;
    private final GetPaymentByOrderIdUseCase getPaymentByOrderIdUseCase;
    private final GetTransactionsByPaymentIdUseCase getTransactionsByPaymentIdUseCase;

    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getPaymentById(
            @PathVariable UUID paymentId,
            @RequestParam(required = false) String guestEmail,
            @AuthenticationPrincipal Jwt jwt) {
        UUID keycloakId = jwt != null ? keycloakId(jwt) : null;
        if (keycloakId == null && (guestEmail == null || guestEmail.isBlank())) {
            throw new PaymentAccessDeniedException();
        }
        Payment payment = getPaymentByIdUseCase.execute(paymentId, keycloakId, guestEmail);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<Payment> getPaymentByOrderId(
            @PathVariable UUID orderId,
            @RequestParam(required = false) String guestEmail,
            @AuthenticationPrincipal Jwt jwt) {
        UUID keycloakId = jwt != null ? keycloakId(jwt) : null;
        if (keycloakId == null && (guestEmail == null || guestEmail.isBlank())) {
            throw new PaymentAccessDeniedException();
        }
        Payment payment = getPaymentByOrderIdUseCase.execute(orderId, keycloakId, guestEmail);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/{paymentId}/transactions")
    public ResponseEntity<List<PaymentTransaction>> getTransactionsByPaymentId(
            @PathVariable UUID paymentId,
            @RequestParam(required = false) String guestEmail,
            @AuthenticationPrincipal Jwt jwt) {
        UUID keycloakId = jwt != null ? keycloakId(jwt) : null;
        if (keycloakId == null && (guestEmail == null || guestEmail.isBlank())) {
            throw new PaymentAccessDeniedException();
        }
        List<PaymentTransaction> transactions =
                getTransactionsByPaymentIdUseCase.execute(paymentId, keycloakId, guestEmail);
        return ResponseEntity.ok(transactions);
    }

    private UUID keycloakId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}