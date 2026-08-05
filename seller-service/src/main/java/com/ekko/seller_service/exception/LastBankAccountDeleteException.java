package com.ekko.seller_service.exception;

import java.util.UUID;

public class LastBankAccountDeleteException extends RuntimeException {

    public LastBankAccountDeleteException(
            UUID accountId,
            UUID sellerId
    ) {
        super(
                "No se puede eliminar la última cuenta bancaria del vendedor "
                        + sellerId
                        + ". Cuenta: "
                        + accountId
        );
    }
}
