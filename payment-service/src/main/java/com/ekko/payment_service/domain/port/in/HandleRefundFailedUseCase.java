package com.ekko.payment_service.domain.port.in;

import com.ekko.payment_service.domain.model.HandleRefundFailedCommand;
import com.ekko.payment_service.domain.model.Refund;

public interface HandleRefundFailedUseCase {

    Refund execute(HandleRefundFailedCommand command);
}