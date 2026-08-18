package com.ekko.product_service.repository;

import com.ekko.product_service.config.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
class ReviewEventRepositoryTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private ReviewEventRepository reviewEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void insertIfAbsent_primeraVez_retorna1() {
        int inserted = reviewEventRepository.insertIfAbsent(UUID.randomUUID());

        assertEquals(1, inserted);
        assertEquals(1L, count());
    }

    @Test
    void insertIfAbsent_duplicado_retorna0YNoInserta() {
        UUID reviewId = UUID.randomUUID();
        reviewEventRepository.insertIfAbsent(reviewId);

        int inserted = reviewEventRepository.insertIfAbsent(reviewId);

        assertEquals(0, inserted);
        assertEquals(1L, count());
    }

    private Long count() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM review_events", Long.class);
    }
}