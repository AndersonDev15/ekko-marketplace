package com.ekko.payment_service.domain.port.in;

import java.util.UUID;

public interface RefreshOnboardingLinkUseCase {

    String execute(UUID vendorId);
}