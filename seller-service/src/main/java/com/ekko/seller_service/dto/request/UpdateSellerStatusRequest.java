package com.ekko.seller_service.dto.request;

import com.ekko.seller_service.enums.SellerStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSellerStatusRequest(
        @NotNull SellerStatus status
) {}