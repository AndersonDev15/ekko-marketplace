package com.ekko.seller_service.support;

import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerBankAccount;
import com.ekko.seller_service.enums.BankAccountType;

import java.util.UUID;

public final class BankAccountTestDataBuilder {

    private UUID id = UUID.randomUUID();
    private Seller seller;
    private String bankName = "Bancolombia";
    private BankAccountType accountType = BankAccountType.SAVINGS;
    private String accountNumber = "1234567890";
    private String accountHolder = "Vendedor Ejemplo";
    private Boolean isPrimary = false;

    private BankAccountTestDataBuilder() {
    }

    public static BankAccountTestDataBuilder aBankAccount() {
        return new BankAccountTestDataBuilder();
    }

    public BankAccountTestDataBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public BankAccountTestDataBuilder withSeller(Seller seller) {
        this.seller = seller;
        return this;
    }

    public BankAccountTestDataBuilder withBankName(String bankName) {
        this.bankName = bankName;
        return this;
    }

    public BankAccountTestDataBuilder withAccountType(BankAccountType accountType) {
        this.accountType = accountType;
        return this;
    }

    public BankAccountTestDataBuilder withAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
        return this;
    }

    public BankAccountTestDataBuilder withAccountHolder(String accountHolder) {
        this.accountHolder = accountHolder;
        return this;
    }

    public BankAccountTestDataBuilder primary() {
        this.isPrimary = true;
        return this;
    }

    public BankAccountTestDataBuilder notPrimary() {
        this.isPrimary = false;
        return this;
    }

    public SellerBankAccount build() {
        return SellerBankAccount.builder()
                .id(id)
                .seller(seller)
                .bankName(bankName)
                .accountType(accountType)
                .accountNumber(accountNumber)
                .accountHolder(accountHolder)
                .isPrimary(isPrimary)
                .build();
    }
}
