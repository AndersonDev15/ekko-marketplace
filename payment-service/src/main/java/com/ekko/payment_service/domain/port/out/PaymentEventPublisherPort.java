package com.ekko.payment_service.domain.port.out;

import com.ekko.payment_service.domain.event.PaymentCancelledEvent;
import com.ekko.payment_service.domain.event.PaymentCompletedEvent;
import com.ekko.payment_service.domain.event.PaymentFailedEvent;
import com.ekko.payment_service.domain.event.PaymentInitiatedEvent;
import com.ekko.payment_service.domain.event.PaymentRefundFailedEvent;
import com.ekko.payment_service.domain.event.PaymentRefundedEvent;
import com.ekko.payment_service.domain.event.TransferFailedEvent;

public interface PaymentEventPublisherPort {

    void publishPaymentInitiated(PaymentInitiatedEvent event);

    void publishPaymentCompleted(PaymentCompletedEvent event);

    void publishPaymentFailed(PaymentFailedEvent event);

    void publishPaymentCancelled(PaymentCancelledEvent event);

    void publishPaymentRefunded(PaymentRefundedEvent event);

    void publishPaymentRefundFailed(PaymentRefundFailedEvent event);

    void publishTransferFailed(TransferFailedEvent event);
}
