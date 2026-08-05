package com.ekko.seller_service.repository;

import com.ekko.seller_service.AbstractPostgresRepositoryTest;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerMetrics;
import com.ekko.seller_service.enums.SellerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SellerMetricsRepositoryTest extends AbstractPostgresRepositoryTest {

    @Autowired
    private SellerMetricsRepository metricsRepository;

    @Autowired
    private SellerRepository sellerRepository;

    @Test
    void findBySellerId_existente_devuelveMetricas() {
        Seller seller = saveSeller("kc-001", "a@ekko.test");
        SellerMetrics metrics = metricsRepository.saveAndFlush(metrics(seller));

        Optional<SellerMetrics> result = metricsRepository.findBySellerId(seller.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(metrics.getId());
    }

    @Test
    void findBySellerId_inexistente_devuelveVacio() {
        Optional<SellerMetrics> result = metricsRepository.findBySellerId(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    void constraintSellerIdUnico_unaSolaFilaDeMetricasPorVendedor() {
        Seller seller = saveSeller("kc-001", "a@ekko.test");
        metricsRepository.saveAndFlush(metrics(seller));

        assertThatThrownBy(() -> metricsRepository.saveAndFlush(metrics(seller)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void constraintFkVendedorInexistente_lanzaViolacion() {
        Seller ghost = Seller.builder().id(UUID.randomUUID()).build();

        assertThatThrownBy(() -> metricsRepository.saveAndFlush(metrics(ghost)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void uniqueSellerIdExisteEnEsquema() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_constraint WHERE conrelid = 'seller_metrics'::regclass AND contype = 'u'",
                Integer.class);

        assertThat(count).isEqualTo(1);
    }

    private Seller saveSeller(String keycloakId, String email) {
        return sellerRepository.saveAndFlush(Seller.builder()
                .keycloakId(keycloakId)
                .storeName("Mi tienda")
                .email(email)
                .status(SellerStatus.ACTIVE)
                .build());
    }

    private static SellerMetrics metrics(Seller seller) {
        return SellerMetrics.builder().seller(seller).build();
    }
}
