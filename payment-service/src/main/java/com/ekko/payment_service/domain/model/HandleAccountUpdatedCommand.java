package com.ekko.payment_service.domain.model;

public record HandleAccountUpdatedCommand(
        String stripeAccountId,
        boolean chargesEnabled,
        boolean payoutsEnabled,
        boolean disabledReasonPresent,
        boolean hasPendingRequirements
) {
}