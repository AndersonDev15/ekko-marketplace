package com.ekko.payment_service.domain.port.in;

public interface RefreshOnboardingLinkByStripeAccountUseCase {

    String execute(String stripeAccountId);
}
