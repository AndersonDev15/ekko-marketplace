package com.ekko.seller_service.repository;

import com.ekko.seller_service.AbstractPostgresRepositoryTest;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerBankAccount;
import com.ekko.seller_service.enums.BankAccountType;
import com.ekko.seller_service.enums.SellerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SellerBankAccountRepositoryTest extends AbstractPostgresRepositoryTest {

    @Autowired
    private SellerBankAccountRepository bankRepository;

    @Autowired
    private SellerRepository sellerRepository;

    @Test
    void findBySellerId_devuelveSoloCuentasDelVendedor() {
        Seller sellerA = saveSeller("kc-001", "a@ekko.test");
        Seller sellerB = saveSeller("kc-002", "b@ekko.test");
        bankRepository.saveAndFlush(account(sellerA, "1", false));
        bankRepository.saveAndFlush(account(sellerA, "2", false));
        bankRepository.saveAndFlush(account(sellerB, "3", false));

        List<SellerBankAccount> result = bankRepository.findBySellerId(sellerA.getId());

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(b -> b.getSeller().getId().equals(sellerA.getId()));
    }

    @Test
    void findByIdAndSellerId_existente_devuelveCuenta() {
        Seller seller = saveSeller("kc-001", "a@ekko.test");
        SellerBankAccount account = bankRepository.saveAndFlush(account(seller, "1", false));

        Optional<SellerBankAccount> result = bankRepository.findByIdAndSellerId(account.getId(), seller.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(account.getId());
    }

    @Test
    void findByIdAndSellerId_deOtroVendedor_devuelveVacio() {
        Seller sellerA = saveSeller("kc-001", "a@ekko.test");
        Seller sellerB = saveSeller("kc-002", "b@ekko.test");
        SellerBankAccount account = bankRepository.saveAndFlush(account(sellerA, "1", false));

        Optional<SellerBankAccount> result = bankRepository.findByIdAndSellerId(account.getId(), sellerB.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void existsBySellerId_devuelveVerdaderoYFalso() {
        Seller sellerA = saveSeller("kc-001", "a@ekko.test");
        Seller sellerB = saveSeller("kc-002", "b@ekko.test");
        bankRepository.saveAndFlush(account(sellerA, "1", false));

        assertThat(bankRepository.existsBySellerId(sellerA.getId())).isTrue();
        assertThat(bankRepository.existsBySellerId(sellerB.getId())).isFalse();
    }

    @Test
    void existsBySellerIdAndBankNameAndAccountNumber_detectaDuplicado() {
        Seller seller = saveSeller("kc-001", "a@ekko.test");
        bankRepository.saveAndFlush(account(seller, "1234567890", false));

        assertThat(bankRepository.existsBySellerIdAndBankNameAndAccountNumber(
                seller.getId(), "Bancolombia", "1234567890")).isTrue();
        assertThat(bankRepository.existsBySellerIdAndBankNameAndAccountNumber(
                seller.getId(), "Bancolombia", "9999999999")).isFalse();
    }

    @Test
    void existsBySellerIdAndBankNameAndAccountNumberAndIdNot_ignoraLaPropiaCuenta() {
        Seller seller = saveSeller("kc-001", "a@ekko.test");
        SellerBankAccount account = bankRepository.saveAndFlush(account(seller, "1234567890", false));

        assertThat(bankRepository.existsBySellerIdAndBankNameAndAccountNumberAndIdNot(
                seller.getId(), "Bancolombia", "1234567890", account.getId())).isFalse();
        assertThat(bankRepository.existsBySellerIdAndBankNameAndAccountNumberAndIdNot(
                seller.getId(), "Bancolombia", "9999999999", account.getId())).isFalse();
    }

    @Test
    void countBySellerId_cuentaCuentas() {
        Seller seller = saveSeller("kc-001", "a@ekko.test");
        bankRepository.saveAndFlush(account(seller, "1", false));
        bankRepository.saveAndFlush(account(seller, "2", false));

        assertThat(bankRepository.countBySellerId(seller.getId())).isEqualTo(2L);
    }

    @Test
    void clearPrimaryBySellerId_limpiaTodasLasPrimarias() {
        Seller seller = saveSeller("kc-001", "a@ekko.test");
        bankRepository.saveAndFlush(account(seller, "1", true));
        bankRepository.saveAndFlush(account(seller, "2", false));

        bankRepository.clearPrimaryBySellerId(seller.getId());

        assertThat(bankRepository.findBySellerId(seller.getId()))
                .allMatch(b -> !b.getIsPrimary());
    }

    @Test
    void constraintUkSellerBankAccount_detectaDuplicado() {
        Seller seller = saveSeller("kc-001", "a@ekko.test");
        bankRepository.saveAndFlush(account(seller, "1234567890", false));

        assertThatThrownBy(() -> bankRepository.saveAndFlush(account(seller, "1234567890", false)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void constraintUkSellerBankAccountPrimary_soloUnaPrimaria() {
        Seller seller = saveSeller("kc-001", "a@ekko.test");
        bankRepository.saveAndFlush(account(seller, "1", true));

        assertThatThrownBy(() -> bankRepository.saveAndFlush(account(seller, "2", true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void constraintFkVendedorInexistente_lanzaViolacion() {
        Seller ghost = Seller.builder().id(UUID.randomUUID()).build();

        assertThatThrownBy(() -> bankRepository.saveAndFlush(account(ghost, "1", false)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void indicesUnicosExistenEnEsquema() {
        Integer constraints = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_constraint WHERE conname IN ('uk_seller_bank_account')", Integer.class);
        Integer indexes = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'uk_seller_bank_account_primary'",
                Integer.class);

        assertThat(constraints).isEqualTo(1);
        assertThat(indexes).isEqualTo(1);
    }

    private Seller saveSeller(String keycloakId, String email) {
        return sellerRepository.saveAndFlush(Seller.builder()
                .keycloakId(keycloakId)
                .storeName("Mi tienda")
                .email(email)
                .status(SellerStatus.ACTIVE)
                .build());
    }

    private static SellerBankAccount account(Seller seller, String accountNumber, boolean primary) {
        return SellerBankAccount.builder()
                .seller(seller)
                .bankName("Bancolombia")
                .accountType(BankAccountType.SAVINGS)
                .accountNumber(accountNumber)
                .accountHolder("Vendedor Ejemplo")
                .isPrimary(primary)
                .build();
    }
}
