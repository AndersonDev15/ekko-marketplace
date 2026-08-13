package com.ekko.payment_service.domain.port.in;

import com.ekko.payment_service.domain.model.HandlePaymentSucceededCommand;
import com.ekko.payment_service.domain.model.Payment;

public interface HandlePaymentSucceededUseCase {

    Payment execute(HandlePaymentSucceededCommand command);
}