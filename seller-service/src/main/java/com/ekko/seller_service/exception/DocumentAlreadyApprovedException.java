package com.ekko.seller_service.exception;

import com.ekko.seller_service.enums.DocumentType;

import java.util.UUID;

public class DocumentAlreadyApprovedException extends RuntimeException {

    public DocumentAlreadyApprovedException(
            DocumentType documentType,
            UUID sellerId
    ) {
        super(
                "Ya existe un documento de tipo "
                        + documentType
                        + " aprobado para el vendedor "
                        + sellerId
        );
    }
}
