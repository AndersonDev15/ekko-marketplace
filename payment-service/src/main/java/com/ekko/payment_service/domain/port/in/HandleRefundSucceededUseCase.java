package com.ekko.payment_service.domain.port.in;

import com.ekko.payment_service.domain.model.HandleRefundSucceededCommand;
import com.ekko.payment_service.domain.model.Refund;

public interface HandleRefundSucceededUseCase {

    Refund execute(HandleRefundSucceededCommand command);
}