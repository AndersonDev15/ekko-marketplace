package com.ekko.seller_service.dto.request;

import com.ekko.seller_service.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SellerDocumentRequest(
        @NotNull DocumentType documentType,
        @NotBlank String documentUrl
) {}