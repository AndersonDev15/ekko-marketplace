package com.ekko.payment_service.domain.port.in;

import com.ekko.payment_service.domain.command.HandlePaymentFailedCommand;
import com.ekko.payment_service.domain.model.Payment;

public interface HandlePaymentFailedUseCase {

    Payment execute(HandlePaymentFailedCommand command);
}