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
class ReviewConfirmationRepositoryTest extends AbstractPostgresRepositoryTest {

    @Autowired
    private ReviewConfirmationRepository confirmationRepository;

    @Test
    void insertIfAbsent_primeraVez_retorna1() {
        UUID reviewId = UUID.randomUUID();

        int inserted = confirmationRepository.insertIfAbsent(reviewId);

        assertThat(inserted).isEqualTo(1);
        assertThat(count()).isEqualTo(1L);
    }

    @Test
    void insertIfAbsent_duplicado_retorna0YNoInserta() {
        UUID reviewId = UUID.randomUUID();
        confirmationRepository.insertIfAbsent(reviewId);

        int inserted = confirmationRepository.insertIfAbsent(reviewId);

        assertThat(inserted).isZero();
        assertThat(count()).isEqualTo(1L);
    }

    private Long count() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM review_confirmations", Long.class);
    }
}