package com.ekko.seller_service.exception;

public class PrimaryBankAccountDeleteException extends RuntimeException {

    public PrimaryBankAccountDeleteException() {
        super(
                "No se puede eliminar la cuenta de banco principal. Primero selecciona otra cuenta como principal."
        );
    }
}