package com.ekko.order_service.infrastructure.persistence.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;

@Schema(description = "Request to cancel an order as guest (email required for verification)")
public record CancelOrderRequest(
        @Schema(description = "Guest email used for the order", example = "guest@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @Email String guestEmail
) {
}