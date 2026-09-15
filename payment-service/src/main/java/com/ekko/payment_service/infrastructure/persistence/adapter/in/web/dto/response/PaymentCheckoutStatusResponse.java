package com.ekko.payment_service.infrastructure.persistence.adapter.in.web.dto.response;

import com.ekko.payment_service.domain.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Payment checkout status response")
public record PaymentCheckoutStatusResponse(
        @Schema(description = "Order UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID orderId,

        @Schema(description = "Payment status", example = "PENDING", allowableValues = {"PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"})
        PaymentStatus status,

        @Schema(description = "Stripe client secret (only present when status is PENDING)", example = "pi_123_secret_abc")
        String clientSecret
) {
}