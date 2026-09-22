package com.ekko.seller_service;

import com.ekko.seller_service.dto.request.SellerBankAccountRequest;
import com.ekko.seller_service.dto.response.SellerBankAccountResponse;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerBankAccount;
import com.ekko.seller_service.enums.BankAccountType;
import com.ekko.seller_service.repository.SellerBankAccountRepository;
import com.ekko.seller_service.service.SellerBankAccountService;
import com.ekko.seller_service.support.ConcurrencyRunner;
import com.ekko.seller_service.support.JwtTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


class SellerBankAccountConcurrencyTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private SellerBankAccountService bankAccountService;

    @Autowired
    private SellerBankAccountRepository bankRepository;

    private Seller seller;

    @BeforeEach
    void seedSeller() {
        // addBankAccount uses validateCanEditProfile which requires PENDING_REVIEW
        seller = insertPendingReviewSeller(JwtTestUtils.SELLER_KEYCLOAK_ID);
    }

    @Test
    void addFirstAccount_concurrente_quedaUnaSolaPrimaria() {
        int threads = 8;

        List<SellerBankAccountResponse> results = ConcurrencyRunner.run(
                "primeras cuentas", threads,
                i -> () -> bankAccountService.addBankAccount(seller.getId(), accountRequest("900" + (10000 + i))));

        assertThat(results).hasSize(threads);

        long primaryInResults = results.stream()
                .filter(SellerBankAccountResponse::isPrimary)
                .count();
        assertThat(primaryInResults).isEqualTo(1);

        long primaryAccounts = bankRepository.findBySellerId(seller.getId())
                .stream()
                .filter(SellerBankAccount::getIsPrimary)
                .count();
        assertThat(primaryAccounts).isEqualTo(1);
        assertThat(bankRepository.countBySellerId(seller.getId())).isEqualTo(threads);
    }

    @Test
    void setPrimary_concurrent_quedaUnaSolaPrimaria() {
        int count = 4;
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(bankAccountService.addBankAccount(seller.getId(), accountRequest("800" + (10000 + i))).id());
        }
        // Change to PENDING_REVIEW for setPrimaryBankAccount (uses validateCanEditProfile)
        jdbcTemplate.update("UPDATE sellers SET status = 'PENDING_REVIEW' WHERE keycloak_id = ?", JwtTestUtils.SELLER_KEYCLOAK_ID);

        List<SellerBankAccountResponse> results = ConcurrencyRunner.run(
                "cambios de primary", 4,
                i -> () -> bankAccountService.setPrimaryBankAccount(seller.getId(), ids.get(i)));

        assertThat(results).hasSize(4);

        long primaryAccounts = bankRepository.findBySellerId(seller.getId())
                .stream()
                .filter(SellerBankAccount::getIsPrimary)
                .count();
        assertThat(primaryAccounts).isEqualTo(1);
    }

    private SellerBankAccountRequest accountRequest(String number) {
        return new SellerBankAccountRequest("Banco Nacional", BankAccountType.SAVINGS, number, "Vendedor Test");
    }
}