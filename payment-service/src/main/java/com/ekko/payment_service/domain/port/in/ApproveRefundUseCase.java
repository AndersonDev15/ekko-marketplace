package com.ekko.payment_service.domain.port.in;

import com.ekko.payment_service.domain.model.Refund;

import java.util.UUID;

public interface ApproveRefundUseCase {
    Refund execute(UUID refundId);
}
