package com.ekko.order_service.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Request to create a new order")
public record CreateOrderRequest(
        @Schema(description = "Customer Keycloak ID (optional, set from JWT if authenticated)")
        UUID customerId,

        @Schema(description = "Guest email for non-authenticated orders", example = "guest@example.com")
        @Email String guestEmail,

        @Schema(description = "Shipping address", requiredMode = Schema.RequiredMode.REQUIRED)
        @Valid @NotNull AddressRequest shippingAddress,

        @Schema(description = "Order items", requiredMode = Schema.RequiredMode.REQUIRED)
        @Valid @NotNull List<ItemRequest> items,

        @Schema(description = "Optional order notes", example = "Please leave at the door")
        String notes
) {

    @Schema(description = "Shipping address details")
    public record AddressRequest(
            @Schema(description = "Recipient full name", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank String fullName,

            @Schema(description = "Phone number", example = "+1-555-123-4567", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank String phone,

            @Schema(description = "Street address", example = "123 Main St", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank String addressLine,

            @Schema(description = "City", example = "New York", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank String city,

            @Schema(description = "State/Province", example = "NY", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank String state,

            @Schema(description = "Country", example = "USA", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank String country,

            @Schema(description = "Postal/ZIP code", example = "10001")
            String postalCode
    ) {
    }

    @Schema(description = "Order item")
    public record ItemRequest(
            @Schema(description = "Product variant ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull UUID variantId,

            @Schema(description = "Quantity", example = "2", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
            @Min(1) long quantity
    ) {
    }
}