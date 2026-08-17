package com.ekko.payment_service.domain.port.in;

import com.ekko.payment_service.domain.command.InitiatePaymentCommand;
import com.ekko.payment_service.domain.model.Payment;

public interface InitiatePaymentUseCase {

    Payment execute(InitiatePaymentCommand command);
}
