package com.ekko.order_service.infrastructure.persistence.adapter.in.web.dto;

import jakarta.validation.constraints.Email;

public record CancelOrderRequest(
        @Email String guestEmail
) {
}