package com.ekko.payment_service.application.exception;

import com.ekko.payment_service.domain.enums.RefundStatus;

import java.util.UUID;

public class RefundAlreadyProcessedException extends RuntimeException {

    public RefundAlreadyProcessedException(UUID refundId, RefundStatus status) {
        super("Refund " + refundId + " cannot be processed because it is in status " + status);
    }
}
