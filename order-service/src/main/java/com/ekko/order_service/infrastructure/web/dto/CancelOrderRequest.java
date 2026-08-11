package com.ekko.order_service.infrastructure.web.dto;

import jakarta.validation.constraints.Email;

public record CancelOrderRequest(
        @Email String guestEmail
) {
}