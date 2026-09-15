package com.ekko.seller_service.dto.request;

import com.ekko.seller_service.enums.BankAccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SellerBankAccountRequest(
        @Schema(description = "Bank name", example = "Banco de Bogotá", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100)
        @NotBlank @Size(max = 100) String bankName,

        @Schema(description = "Account type", example = "SAVINGS", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull BankAccountType accountType,

        @Schema(description = "Account number", example = "1234567890", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 50)
        @NotBlank @Size(max = 50) String accountNumber,

        @Schema(description = "Account holder name", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 255)
        @NotBlank @Size(max = 255) String accountHolder
) {}
