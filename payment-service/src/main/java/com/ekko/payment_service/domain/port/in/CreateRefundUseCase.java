package com.ekko.payment_service.domain.port.in;

import com.ekko.payment_service.domain.model.CreateRefundCommand;
import com.ekko.payment_service.domain.model.Refund;

public interface CreateRefundUseCase {

    Refund execute(CreateRefundCommand command);
}