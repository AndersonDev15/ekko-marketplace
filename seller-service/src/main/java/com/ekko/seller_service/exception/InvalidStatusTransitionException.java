package com.ekko.seller_service.exception;

import com.ekko.seller_service.enums.SellerStatus;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(SellerStatus from, SellerStatus to) {
        super("Cannot transition seller status from %s to %s".formatted(from, to));
    }
}
