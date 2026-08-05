package com.ekko.seller_service.support;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrimaryEntityPolicyTest {

    private final PrimaryEntityPolicy policy = new PrimaryEntityPolicy();

    @Test
    void promote_ejecutaAmbosRunnablesEnOrden() {
        List<String> order = new ArrayList<>();

        policy.promote(() -> order.add("clear"), () -> order.add("mark"));

        assertEquals(List.of("clear", "mark"), order);
    }

    @Test
    void promote_ejecutaAccionesInclusoSinOperacionesPrevias() {
        boolean[] marked = {false};

        policy.promote(() -> {
        }, () -> marked[0] = true);

        assertEquals(true, marked[0]);
    }
}
