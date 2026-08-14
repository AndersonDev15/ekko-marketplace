package com.ekko.payment_service.infrastructure.web.webhook;

import com.ekko.payment_service.domain.model.HandleAccountUpdatedCommand;
import com.ekko.payment_service.domain.port.in.HandleAccountUpdatedUseCase;
import com.stripe.model.Account;
import com.stripe.model.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VendorAccountHandler {

    private final HandleAccountUpdatedUseCase handleAccountUpdatedUseCase;

    public void onUpdated(Event stripeEvent, Account account) {
        Account.Requirements requirements = account.getRequirements();

        boolean disabledReasonPresent = requirements != null
                && requirements.getDisabledReason() != null;

        boolean hasPendingRequirements = requirements != null
                && ((requirements.getCurrentlyDue() != null && !requirements.getCurrentlyDue().isEmpty())
                || (requirements.getPastDue() != null && !requirements.getPastDue().isEmpty()));

        handleAccountUpdatedUseCase.execute(new HandleAccountUpdatedCommand(
                account.getId(),
                Boolean.TRUE.equals(account.getChargesEnabled()),
                Boolean.TRUE.equals(account.getPayoutsEnabled()),
                disabledReasonPresent,
                hasPendingRequirements));
    }
}
