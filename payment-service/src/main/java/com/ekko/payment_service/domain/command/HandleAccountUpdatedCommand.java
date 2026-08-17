package com.ekko.payment_service.domain.command;

public record HandleAccountUpdatedCommand(
        String stripeAccountId,
        boolean chargesEnabled,
        boolean payoutsEnabled,
        boolean disabledReasonPresent,
        boolean hasPendingRequirements
) {
}