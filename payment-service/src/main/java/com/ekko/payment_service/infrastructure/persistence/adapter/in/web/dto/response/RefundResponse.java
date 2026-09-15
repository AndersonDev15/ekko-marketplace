package com.ekko.payment_service.infrastructure.persistence.adapter.in.web.dto.response;

import com.ekko.payment_service.domain.model.Refund;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Refund response")
public record RefundResponse(
        @Schema(description = "Refund UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID id,

        @Schema(description = "Payment UUID", example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
        UUID paymentId,

        @Schema(description = "Stripe refund ID", example = "re_1234567890abcdef")
        String stripeRefundId,

        @Schema(description = "Refund amount", example = "50.00")
        BigDecimal amount,

        @Schema(description = "Refund reason", example = "DUPLICATE", allowableValues = {"DUPLICATE", "FRAUDULENT", "REQUESTED_BY_CUSTOMER"})
        String reason,

        @Schema(description = "Refund status", example = "PENDING", allowableValues = {"PENDING", "SUCCEEDED", "FAILED", "CANCELLED"})
        String status,

        @Schema(description = "Optional notes", example = "Customer requested refund")
        String notes,

        @Schema(description = "Refund creation timestamp")
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