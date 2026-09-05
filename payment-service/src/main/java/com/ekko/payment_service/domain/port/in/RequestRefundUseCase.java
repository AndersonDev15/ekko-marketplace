package com.ekko.payment_service.domain.port.in;

import com.ekko.payment_service.domain.command.CreateRefundCommand;
import com.ekko.payment_service.domain.model.Refund;

public interface RequestRefundUseCase {

    Refund execute(CreateRefundCommand command);
}
