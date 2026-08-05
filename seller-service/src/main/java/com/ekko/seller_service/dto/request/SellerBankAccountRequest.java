package com.ekko.seller_service.dto.request;

import com.ekko.seller_service.enums.BankAccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SellerBankAccountRequest(
        @NotBlank @Size(max = 100) String bankName,
        @NotNull BankAccountType accountType,
        @NotBlank @Size(max = 50) String accountNumber,
        @NotBlank @Size(max = 255) String accountHolder
) {}
