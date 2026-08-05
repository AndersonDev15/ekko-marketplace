package com.ekko.seller_service.exception;

import java.util.UUID;

public class DuplicateSellerBankAccountException extends SellerServiceException {

    public DuplicateSellerBankAccountException(UUID sellerId, String accountNumber) {
        super("Bank account already exists for seller: "
                + sellerId + " with account number: "
                + accountNumber);
    }
}
