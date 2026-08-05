package com.ekko.seller_service.exception;

import java.util.UUID;

public class PrimaryAddressDeleteException extends RuntimeException {

    public PrimaryAddressDeleteException() {
        super(
                "No se puede eliminar la dirección principal. Primero selecciona otra dirección como principal."
        );
    }
}
