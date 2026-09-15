package com.ekko.seller_service.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Document type", allowableValues = {"ID_CARD", "RUT", "BUSINESS_LICENSE", "BANK_CERTIFICATE"})
public enum DocumentType {
    ID_CARD,
    RUT,
    BUSINESS_LICENSE,
    BANK_CERTIFICATE
}
