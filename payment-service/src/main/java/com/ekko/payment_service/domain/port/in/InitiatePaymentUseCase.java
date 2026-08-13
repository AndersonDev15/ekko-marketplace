package com.ekko.payment_service.domain.port.in;

import com.ekko.payment_service.domain.model.InitiatePaymentCommand;
import com.ekko.payment_service.domain.model.Payment;

public interface InitiatePaymentUseCase {

    Payment execute(InitiatePaymentCommand command);
}
