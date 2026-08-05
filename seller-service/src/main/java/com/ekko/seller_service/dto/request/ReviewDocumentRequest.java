package com.ekko.seller_service.dto.request;

import com.ekko.seller_service.enums.DocumentStatus;
import jakarta.validation.constraints.NotNull;

public record ReviewDocumentRequest(
        @NotNull DocumentStatus status,
        String notes
) {}