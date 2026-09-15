package com.ekko.seller_service.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Bank account type", allowableValues = {"SAVINGS", "CHECKING"})
public enum BankAccountType {
    SAVINGS,
    CHECKING
}
