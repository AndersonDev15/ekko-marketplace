package com.ekko.payment_service.domain.port.in;

import com.ekko.payment_service.domain.command.HandleAccountUpdatedCommand;
import com.ekko.payment_service.domain.model.VendorStripeAccount;

public interface HandleAccountUpdatedUseCase {

    VendorStripeAccount execute(HandleAccountUpdatedCommand command);
}