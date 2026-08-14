package com.ekko.payment_service.domain.port.in;

import com.ekko.payment_service.domain.model.HandleAccountUpdatedCommand;
import com.ekko.payment_service.domain.model.VendorStripeAccount;

public interface HandleAccountUpdatedUseCase {

    VendorStripeAccount execute(HandleAccountUpdatedCommand command);
}