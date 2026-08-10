package com.ekko.order_service.domain.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderNumberGeneratorTest {

    private final OrderNumberGenerator generator = new OrderNumberGenerator();

    @Test
    void generateReturnsOrderNumberWithExpectedFormat() {
        String orderNumber = generator.generate();

        assertTrue(orderNumber.matches("EKK-\\d{8}-[A-Z0-9]{4}"),
                "Unexpected format: " + orderNumber);
    }

    @Test
    void generateProducesUniqueValues() {
        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            generated.add(generator.generate());
        }
        assertEquals(100, generated.size());
    }

    @Test
    void randomSuffixIsAlwaysUpperCaseAlphanumeric() {
        for (int i = 0; i < 100; i++) {
            String generated = generator.generate();
            String suffix = generated.substring(generated.length() - 4);
            assertTrue(suffix.matches("[A-Z0-9]{4}"),
                    "Invalid suffix: " + suffix + " in " + generated);
        }
    }
}