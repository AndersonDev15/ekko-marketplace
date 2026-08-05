package com.ekko.seller_service.exception;

import com.ekko.seller_service.enums.DocumentType;

import java.util.UUID;

public class DocumentAlreadyPendingException extends RuntimeException {

    public DocumentAlreadyPendingException(
            DocumentType documentType,
            UUID sellerId
    ) {
        super(
                "Ya existe un documento de tipo "
                        + documentType
                        + " pendiente de revisión para el vendedor "
                        + sellerId
        );
    }
}
