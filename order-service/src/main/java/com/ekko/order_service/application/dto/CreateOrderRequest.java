package com.ekko.order_service.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        UUID customerId,
        @Email String guestEmail,
        @Valid @NotNull AddressRequest shippingAddress,
        @Valid @NotNull List<ItemRequest> items,
        String notes
) {

    public record AddressRequest(
            @NotBlank String fullName,
            @NotBlank String phone,
            @NotBlank String addressLine,
            @NotBlank String city,
            @NotBlank String state,
            @NotBlank String country,
            String postalCode
    ) {
    }

    public record ItemRequest(
            @NotNull UUID variantId,
            @Min(1) long quantity,
            @Positive BigDecimal unitPrice
    ) {
    }
}