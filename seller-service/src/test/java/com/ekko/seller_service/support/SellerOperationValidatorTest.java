package com.ekko.seller_service.support;

import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.enums.SellerStatus;
import com.ekko.seller_service.exception.SellerPendingException;
import com.ekko.seller_service.exception.SellerSuspendedException;
import org.junit.jupiter.api.Test;

import static com.ekko.seller_service.support.SellerTestDataBuilder.aSeller;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SellerOperationValidatorTest {

    private final SellerOperationValidator validator = new SellerOperationValidator();

    @Test
    void vendedorActivo_noLanzaExcepcion() {
        Seller seller = aSeller().active().build();

        assertDoesNotThrow(() -> validator.validateCanOperate(seller));
    }

    @Test
    void vendedorSuspendido_lanzaSellerSuspended() {
        Seller seller = aSeller().suspended().build();

        assertThrows(SellerSuspendedException.class, () -> validator.validateCanOperate(seller));
    }

    @Test
    void vendedorPendingReview_lanzaSellerPending() {
        Seller seller = aSeller().pendingReview().build();

        assertThrows(SellerPendingException.class, () -> validator.validateCanOperate(seller));
    }

    @Test
    void pendingReviewConStatusNull_noLanza() {
        Seller seller = aSeller().withStatus(null).build();

        assertDoesNotThrow(() -> validator.validateCanOperate(seller));
    }
}
