package com.ekko.seller_service.repository;

import com.ekko.seller_service.entity.SellerBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SellerBankAccountRepository extends JpaRepository<SellerBankAccount, UUID> {
    List<SellerBankAccount> findBySellerId(UUID sellerId);

    Optional<SellerBankAccount> findByIdAndSellerId(UUID id, UUID sellerId);

    boolean existsBySellerId(UUID sellerId);
    boolean existsBySellerIdAndBankNameAndAccountNumber(
            UUID sellerId,
            String bankName,
            String accountNumber
    );
    boolean existsBySellerIdAndBankNameAndAccountNumberAndIdNot(
            UUID sellerId,
            String bankName,
            String accountNumber,
            UUID accountId
    );
    long countBySellerId(UUID sellerId);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE SellerBankAccount b
        SET b.isPrimary = false
        WHERE b.seller.id = :sellerId
    """)
    void clearPrimaryBySellerId(UUID sellerId);
}
