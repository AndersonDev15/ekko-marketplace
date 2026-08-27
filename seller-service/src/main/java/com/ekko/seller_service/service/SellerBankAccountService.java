package com.ekko.seller_service.service;

import com.ekko.seller_service.support.PrimaryEntityPolicy;
import com.ekko.seller_service.support.SellerOperationValidator;
import com.ekko.seller_service.dto.request.SellerBankAccountRequest;
import com.ekko.seller_service.dto.response.SellerBankAccountResponse;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerBankAccount;
import com.ekko.seller_service.exception.*;
import com.ekko.seller_service.mapper.SellerMapper;
import com.ekko.seller_service.repository.SellerBankAccountRepository;
import com.ekko.seller_service.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerBankAccountService {

    private final SellerRepository sellerRepository;
    private final SellerBankAccountRepository bankRepository;
    private final SellerOperationValidator validator;
    private final PrimaryEntityPolicy primaryPolicy;
    private final SellerMapper sellerMapper;
    private static final String UNIQUE_VIOLATION = "23505";

    @Transactional
    public SellerBankAccountResponse addBankAccount(UUID sellerId, SellerBankAccountRequest request) {
        Seller seller = sellerRepository.findByIdForUpdate(sellerId)
                .orElseThrow(() -> new SellerNotFoundException(sellerId));

        validator.validateCanEditProfile(seller);

        boolean exists = bankRepository.existsBySellerIdAndBankNameAndAccountNumber(
                sellerId, request.bankName(), request.accountNumber());

        if (exists) {
            throw new DuplicateSellerBankAccountException(sellerId, request.accountNumber());
        }

        boolean isFirst = !bankRepository.existsBySellerId(sellerId);

        SellerBankAccount account = SellerBankAccount.builder()
                .seller(seller)
                .bankName(request.bankName())
                .accountType(request.accountType())
                .accountNumber(request.accountNumber())
                .accountHolder(request.accountHolder())
                .isPrimary(isFirst)
                .build();

        try {
            return sellerMapper.toBankAccountResponse(bankRepository.save(account));
        } catch (DataIntegrityViolationException e) {
            Throwable cause = e.getMostSpecificCause();
            if (cause instanceof SQLException sql
                    && UNIQUE_VIOLATION.equals(sql.getSQLState())
                    && sql.getMessage().contains("uk_seller_bank_account")) {
                throw new DuplicateSellerBankAccountException(sellerId, request.accountNumber());
            }
            throw e;
        }
    }

    @Transactional
    public SellerBankAccountResponse updateBankAccount(UUID sellerId, UUID accountId, SellerBankAccountRequest request) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerNotFoundException(sellerId));

        validator.validateCanEditProfile(seller);

        SellerBankAccount account = bankRepository
                .findByIdAndSellerId(accountId, sellerId)
                .orElseThrow(() -> new SellerBankAccountNotFoundException(accountId, sellerId));

        if (bankRepository.existsBySellerIdAndBankNameAndAccountNumberAndIdNot(
                sellerId, request.bankName(), request.accountNumber(), accountId)) {
            throw new DuplicateSellerBankAccountException(sellerId, request.accountNumber());
        }

        account.setBankName(request.bankName());
        account.setAccountType(request.accountType());
        account.setAccountNumber(request.accountNumber());
        account.setAccountHolder(request.accountHolder());

        return sellerMapper.toBankAccountResponse(bankRepository.save(account));
    }

    @Transactional(readOnly = true)
    public List<SellerBankAccountResponse> getMyBankAccounts(UUID sellerId) {
        return bankRepository.findBySellerId(sellerId)
                .stream()
                .map(sellerMapper::toBankAccountResponse)
                .toList();
    }

    @Transactional
    public void deleteBankAccount(UUID sellerId, UUID accountId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerNotFoundException(sellerId));

        validator.validateCanEditProfile(seller);

        SellerBankAccount account = bankRepository
                .findByIdAndSellerId(accountId, sellerId)
                .orElseThrow(() -> new SellerBankAccountNotFoundException(accountId, sellerId));

        long total = bankRepository.countBySellerId(sellerId);

        if (total == 1) {
            throw new LastBankAccountDeleteException(accountId, sellerId);
        }

        if (Boolean.TRUE.equals(account.getIsPrimary())) {
            throw new PrimaryBankAccountDeleteException();
        }

        bankRepository.delete(account);
    }

    @Transactional
    public SellerBankAccountResponse setPrimaryBankAccount(UUID sellerId, UUID accountId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerNotFoundException(sellerId));

        validator.validateCanEditProfile(seller);

        SellerBankAccount account = bankRepository
                .findByIdAndSellerId(accountId, sellerId)
                .orElseThrow(() -> new SellerBankAccountNotFoundException(accountId, sellerId));

        if (Boolean.TRUE.equals(account.getIsPrimary())) {
            return sellerMapper.toBankAccountResponse(account);
        }

        primaryPolicy.promote(
                () -> bankRepository.clearPrimaryBySellerId(sellerId),
                () -> account.setIsPrimary(true)
        );

        return sellerMapper.toBankAccountResponse(bankRepository.save(account));
    }
}