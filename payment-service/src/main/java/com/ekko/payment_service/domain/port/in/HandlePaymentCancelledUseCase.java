package com.ekko.payment_service.domain.port.in;

import com.ekko.payment_service.domain.command.HandlePaymentCancelledCommand;
import com.ekko.payment_service.domain.model.Payment;

public interface HandlePaymentCancelledUseCase {

    Payment execute(HandlePaymentCancelledCommand command);
}