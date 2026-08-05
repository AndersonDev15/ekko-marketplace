package com.ekko.seller_service.repository;

import com.ekko.seller_service.AbstractPostgresRepositoryTest;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.enums.SellerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SellerRepositoryTest extends AbstractPostgresRepositoryTest {

    @Autowired
    private SellerRepository sellerRepository;

    @Test
    void findByKeycloakId_vendedorExistente_devuelveSeller() {
        Seller seller = seller("kc-001", "seller-001@ekko.test");
        sellerRepository.saveAndFlush(seller);

        Optional<Seller> result = sellerRepository.findByKeycloakId("kc-001");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("seller-001@ekko.test");
    }

    @Test
    void findByKeycloakId_vendedorInexistente_devuelveVacio() {
        Optional<Seller> result = sellerRepository.findByKeycloakId("no-existe");

        assertThat(result).isEmpty();
    }

    @Test
    void h5_emailDebeSerUnico() {
        sellerRepository.saveAndFlush(seller("kc-001", "duplicado@ekko.test"));

        Seller duplicate = seller("kc-002", "duplicado@ekko.test");

        assertThatThrownBy(() -> sellerRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void keycloakIdDebeSerUnico() {
        sellerRepository.saveAndFlush(seller("kc-001", "seller-001@ekko.test"));

        Seller duplicate = seller("kc-001", "seller-002@ekko.test");

        assertThatThrownBy(() -> sellerRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findAllFiltered_filtraPorEstado() {
        sellerRepository.saveAndFlush(seller("kc-001", "a@ekko.test", SellerStatus.ACTIVE));
        sellerRepository.saveAndFlush(seller("kc-002", "b@ekko.test", SellerStatus.SUSPENDED));
        sellerRepository.saveAndFlush(seller("kc-003", "c@ekko.test", SellerStatus.PENDING_REVIEW));

        Page<Seller> result = sellerRepository.findAllFiltered(
                SellerStatus.SUSPENDED, null, null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getKeycloakId()).isEqualTo("kc-002");
    }

    @Test
    void findAllFiltered_filtraPorRangoDeFechas() {
        Seller older = sellerRepository.saveAndFlush(seller("kc-001", "old@ekko.test"));
        sellerRepository.saveAndFlush(seller("kc-002", "new@ekko.test"));
        jdbcTemplate.update("UPDATE sellers SET created_at = ? WHERE id = ?",
                LocalDateTime.now().minusDays(30), older.getId());

        Page<Seller> result = sellerRepository.findAllFiltered(
                null, LocalDateTime.now().minusDays(10), null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getKeycloakId()).isEqualTo("kc-002");
    }

    @Test
    void findAllFiltered_paginacion() {
        for (int i = 0; i < 5; i++) {
            sellerRepository.saveAndFlush(seller("kc-00" + i, "seller-" + i + "@ekko.test"));
        }

        Page<Seller> firstPage = sellerRepository.findAllFiltered(
                null, null, null, PageRequest.of(0, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
    }

    @Test
    void findByIdForUpdate_devuelveVendedor() {
        Seller seller = sellerRepository.saveAndFlush(seller("kc-001", "seller-001@ekko.test"));

        Optional<Seller> result = sellerRepository.findByIdForUpdate(seller.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(seller.getId());
    }

    private static Seller seller(String keycloakId, String email) {
        return seller(keycloakId, email, SellerStatus.ACTIVE);
    }

    private static Seller seller(String keycloakId, String email, SellerStatus status) {
        return Seller.builder()
                .keycloakId(keycloakId)
                .storeName("Mi tienda")
                .email(email)
                .status(status)
                .build();
    }
}
