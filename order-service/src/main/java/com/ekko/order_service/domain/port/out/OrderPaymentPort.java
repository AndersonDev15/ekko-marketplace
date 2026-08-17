package com.ekko.order_service.domain.port.out;

import com.ekko.order_service.domain.model.PaymentData;

import java.util.UUID;

public interface OrderPaymentPort {

    void recordCompletedPayment(UUID orderId, PaymentData paymentData);
}