package com.ekko.payment_service.domain.port.out;

import com.ekko.payment_service.domain.model.PaymentCancelledEvent;
import com.ekko.payment_service.domain.model.PaymentCompletedEvent;
import com.ekko.payment_service.domain.model.PaymentFailedEvent;
import com.ekko.payment_service.domain.model.PaymentInitiatedEvent;
import com.ekko.payment_service.domain.model.PaymentRefundFailedEvent;
import com.ekko.payment_service.domain.model.PaymentRefundedEvent;

public interface PaymentEventPublisherPort {

    void publishPaymentInitiated(PaymentInitiatedEvent event);

    void publishPaymentCompleted(PaymentCompletedEvent event);

    void publishPaymentFailed(PaymentFailedEvent event);

    void publishPaymentCancelled(PaymentCancelledEvent event);

    void publishPaymentRefunded(PaymentRefundedEvent event);

    void publishPaymentRefundFailed(PaymentRefundFailedEvent event);
}
