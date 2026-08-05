package com.ekko.seller_service.exception;

import java.util.UUID;

public class DocumentAlreadyReviewedException extends RuntimeException {
    public DocumentAlreadyReviewedException(UUID documentId) {
        super("Document %s has already been reviewed".formatted(documentId));
    }
}
