package com.ekko.payment_service.infrastructure.web.dto;

import com.ekko.payment_service.domain.model.Refund;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RefundResponse(
        UUID id,
        UUID paymentId,
        String stripeRefundId,
        BigDecimal amount,
        String reason,
        String status,
        String notes,
        LocalDateTime createdAt
) {

    public static RefundResponse from(Refund refund) {
        return new RefundResponse(
                refund.getId(),
                refund.getPaymentId(),
                refund.getStripeRefundId(),
                refund.getAmount(),
                refund.getReason().name(),
                refund.getStatus().name(),
                refund.getNotes(),
                refund.getCreatedAt());
    }
}