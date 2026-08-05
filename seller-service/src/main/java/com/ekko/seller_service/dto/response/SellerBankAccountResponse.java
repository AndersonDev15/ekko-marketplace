package com.ekko.seller_service.dto.response;

import com.ekko.seller_service.enums.BankAccountType;

import java.time.LocalDateTime;
import java.util.UUID;

public record SellerBankAccountResponse(
        UUID id,
        String bankName,
        BankAccountType accountType,
        String accountNumber,
        String accountHolder,
        Boolean isPrimary,
        LocalDateTime createdAt
) {}