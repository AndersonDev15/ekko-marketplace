package com.ekko.payment_service.infrastructure.persistence.adapter.in.web;

import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.model.PaymentTransaction;
import com.ekko.payment_service.domain.port.in.GetPaymentByIdForAdminUseCase;
import com.ekko.payment_service.domain.port.in.GetPaymentByOrderIdForAdminUseCase;
import com.ekko.payment_service.domain.port.in.GetTransactionsByPaymentIdForAdminUseCase;
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
public class AdminPaymentController {

    private final GetPaymentByIdForAdminUseCase getPaymentByIdForAdminUseCase;
    private final GetPaymentByOrderIdForAdminUseCase getPaymentByOrderIdForAdminUseCase;
    private final GetTransactionsByPaymentIdForAdminUseCase getTransactionsByPaymentIdForAdminUseCase;

    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable UUID paymentId) {
        Payment payment = getPaymentByIdForAdminUseCase.execute(paymentId);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<Payment> getPaymentByOrderId(@PathVariable UUID orderId) {
        Payment payment = getPaymentByOrderIdForAdminUseCase.execute(orderId);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/{paymentId}/transactions")
    public ResponseEntity<List<PaymentTransaction>> getTransactionsByPaymentId(@PathVariable UUID paymentId) {
        List<PaymentTransaction> transactions = getTransactionsByPaymentIdForAdminUseCase.execute(paymentId);
        return ResponseEntity.ok(transactions);
    }
}