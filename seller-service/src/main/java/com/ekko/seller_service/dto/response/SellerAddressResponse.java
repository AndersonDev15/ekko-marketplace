package com.ekko.seller_service.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record SellerAddressResponse(
        UUID id,
        String addressLine,
        String city,
        String state,
        String country,
        String postalCode,
        Boolean isPrimary,
        LocalDateTime createdAt
) {}
