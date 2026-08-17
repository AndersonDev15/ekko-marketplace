package com.ekko.payment_service.domain.command;

import java.util.UUID;

public record CreateVendorAccountCommand(
        UUID vendorId,
        String country,
        String email
) {
}