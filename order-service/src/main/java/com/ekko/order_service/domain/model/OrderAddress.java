package com.ekko.order_service.domain.model;

import java.util.UUID;

public record OrderAddress(
        UUID id,
        String fullName,
        String phone,
        String addressLine,
        String city,
        String state,
        String country,
        String postalCode
) {
}