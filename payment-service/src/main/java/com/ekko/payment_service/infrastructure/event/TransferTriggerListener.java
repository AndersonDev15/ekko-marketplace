package com.ekko.payment_service.infrastructure.event;

import com.ekko.payment_service.domain.event.PaymentSucceededInternalEvent;
import com.ekko.payment_service.domain.port.in.ProcessTransfersUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransferTriggerListener {

    private final ProcessTransfersUseCase processTransfersUseCase;

    @EventListener
    public void onPaymentSucceeded(PaymentSucceededInternalEvent event) {
        processTransfersUseCase.execute(event.paymentId());
    }
}