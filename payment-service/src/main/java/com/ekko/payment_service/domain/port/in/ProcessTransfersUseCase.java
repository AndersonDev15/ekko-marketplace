package com.ekko.payment_service.domain.port.in;

import java.util.UUID;

public interface ProcessTransfersUseCase {

    void execute(UUID paymentId);
}