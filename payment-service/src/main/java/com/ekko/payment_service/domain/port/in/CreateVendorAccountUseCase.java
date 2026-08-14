package com.ekko.payment_service.domain.port.in;

import com.ekko.payment_service.domain.model.CreateVendorAccountCommand;
import com.ekko.payment_service.domain.model.VendorAccountResult;

public interface CreateVendorAccountUseCase {

    VendorAccountResult execute(CreateVendorAccountCommand command);
}