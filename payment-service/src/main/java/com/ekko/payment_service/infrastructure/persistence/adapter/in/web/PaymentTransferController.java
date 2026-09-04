package com.ekko.payment_service.infrastructure.persistence.adapter.in.web;

import com.ekko.payment_service.domain.port.in.ProcessTransfersUseCase;
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
public class PaymentTransferController {

    private final ProcessTransfersUseCase processTransfersUseCase;

    @PostMapping("/{paymentId}/retry")
    public ResponseEntity<Void> retry(@PathVariable UUID paymentId) {
        processTransfersUseCase.execute(paymentId);
        return ResponseEntity.noContent().build();
    }
}
