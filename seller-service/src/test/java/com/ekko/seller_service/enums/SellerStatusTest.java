package com.ekko.seller_service.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SellerStatusTest {

    @ParameterizedTest
    @CsvSource({
            "PENDING_REVIEW, ACTIVE, true",
            "PENDING_REVIEW, SUSPENDED, false",
            "PENDING_REVIEW, PENDING_REVIEW, false",
            "ACTIVE, SUSPENDED, true",
            "ACTIVE, PENDING_REVIEW, false",
            "ACTIVE, ACTIVE, false",
            "SUSPENDED, ACTIVE, true",
            "SUSPENDED, SUSPENDED, false",
            "SUSPENDED, PENDING_REVIEW, false"
    })
    void canTransitionTo_verificaTransiciones(SellerStatus from, SellerStatus to, boolean allowed) {
        if (allowed) {
            assertTrue(from.canTransitionTo(to));
        } else {
            assertFalse(from.canTransitionTo(to));
        }
    }
}
