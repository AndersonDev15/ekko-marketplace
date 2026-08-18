package com.ekko.seller_service.repository;

import com.ekko.seller_service.AbstractPostgresRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductEventRepositoryTest extends AbstractPostgresRepositoryTest {

    @Autowired
    private ProductEventRepository productEventRepository;

    @Test
    void insertIfAbsent_primeraVez_retorna1() {
        int inserted = productEventRepository.insertIfAbsent(UUID.randomUUID(), "PUBLISHED");

        assertThat(inserted).isEqualTo(1);
        assertThat(count()).isEqualTo(1L);
    }

    @Test
    void insertIfAbsent_duplicadoMismoTipo_retorna0YNoInserta() {
        UUID productId = UUID.randomUUID();
        productEventRepository.insertIfAbsent(productId, "PUBLISHED");

        int inserted = productEventRepository.insertIfAbsent(productId, "PUBLISHED");

        assertThat(inserted).isZero();
        assertThat(count()).isEqualTo(1L);
    }

    @Test
    void insertIfAbsent_mismoProductoOtroTipo_esIndependiente() {
        UUID productId = UUID.randomUUID();
        productEventRepository.insertIfAbsent(productId, "PUBLISHED");

        int inserted = productEventRepository.insertIfAbsent(productId, "DEACTIVATED");

        assertThat(inserted).isEqualTo(1);
        assertThat(count()).isEqualTo(2L);
    }

    private Long count() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM product_events", Long.class);
    }
}