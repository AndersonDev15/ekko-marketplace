package com.ekko.seller_service.dto.response;

import com.ekko.seller_service.enums.BankAccountType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record SellerBankAccountResponse(
        @Schema(description = "Bank account UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Bank name", example = "Banco de Bogotá")
        String bankName,

        @Schema(description = "Account type", example = "SAVINGS")
        BankAccountType accountType,

        @Schema(description = "Account number", example = "1234567890")
        String accountNumber,

        @Schema(description = "Account holder name", example = "John Doe")
        String accountHolder,

        @Schema(description = "Whether this is the primary account", example = "true")
        Boolean isPrimary,

        @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00")
        LocalDateTime createdAt
) {}