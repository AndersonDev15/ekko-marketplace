package com.ekko.payment_service.domain.port.in;

import com.ekko.payment_service.domain.model.Refund;

import java.util.UUID;

public interface RejectRefundUseCase {
    Refund execute(UUID refundId);
}
