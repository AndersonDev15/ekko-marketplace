package com.ekko.order_service.domain.port.in;

import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.model.PaymentData;

import java.util.UUID;

public interface PaymentCallbackUseCase {

    Order onPaymentCompleted(UUID orderId, PaymentData paymentData);

    Order onPaymentFailed(UUID orderId);
}