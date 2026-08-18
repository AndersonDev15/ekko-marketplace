package com.ekko.product_service.repository;

import com.ekko.product_service.config.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
class OrderEventRepositoryTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private OrderEventRepository orderEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void insertIfAbsent_primeraVez_retorna1() {
        int inserted = orderEventRepository.insertIfAbsent(UUID.randomUUID(), "CONFIRMED");

        assertEquals(1, inserted);
        assertEquals(1L, count());
    }

    @Test
    void insertIfAbsent_duplicadoMismoTipo_retorna0YNoInserta() {
        UUID orderId = UUID.randomUUID();
        orderEventRepository.insertIfAbsent(orderId, "CONFIRMED");

        int inserted = orderEventRepository.insertIfAbsent(orderId, "CONFIRMED");

        assertEquals(0, inserted);
        assertEquals(1L, count());
    }

    @Test
    void insertIfAbsent_mismaOrdenOtroTipo_esIndependiente() {
        UUID orderId = UUID.randomUUID();
        orderEventRepository.insertIfAbsent(orderId, "CONFIRMED");

        int inserted = orderEventRepository.insertIfAbsent(orderId, "CANCELLED");

        assertEquals(1, inserted);
        assertEquals(2L, count());
    }

    private Long count() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM order_events", Long.class);
    }
}