package com.ekko.payment_service.infrastructure.persistence.adapter.in.web.webhook;

import com.ekko.payment_service.domain.command.HandleRefundFailedCommand;
import com.ekko.payment_service.domain.command.HandleRefundSucceededCommand;
import com.ekko.payment_service.domain.port.in.HandleRefundFailedUseCase;
import com.ekko.payment_service.domain.port.in.HandleRefundSucceededUseCase;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefundHandler {

    private static final String REFUND_STATUS_SUCCEEDED = "succeeded";

    private final HandleRefundSucceededUseCase handleRefundSucceededUseCase;
    private final HandleRefundFailedUseCase handleRefundFailedUseCase;

    public void onUpdated(Event stripeEvent, Refund refund) {
        if (!REFUND_STATUS_SUCCEEDED.equals(refund.getStatus())) {
            return;
        }
        handleRefundSucceededUseCase.execute(new HandleRefundSucceededCommand(
                refund.getId(),
                stripeEvent.getId()));
    }

    public void onFailed(Event stripeEvent, Refund refund) {
        handleRefundFailedUseCase.execute(new HandleRefundFailedCommand(
                refund.getId(),
                stripeEvent.getId()));
    }
}
