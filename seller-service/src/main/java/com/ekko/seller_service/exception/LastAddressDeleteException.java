package com.ekko.seller_service.exception;

import java.util.UUID;

public class LastAddressDeleteException extends RuntimeException {

    public LastAddressDeleteException(
            UUID addressId,
            UUID sellerId
    ) {
        super(
                "No se puede eliminar la última dirección del vendedor "
                        + sellerId
                        + ". Dirección: "
                        + addressId
        );
    }
}