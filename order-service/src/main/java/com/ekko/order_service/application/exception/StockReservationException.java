package com.ekko.order_service.application.exception;

public class StockReservationException extends RuntimeException {

    public StockReservationException(String message) {
        super(message);
    }

    public StockReservationException(String message, Throwable cause) {
        super(message, cause);
    }
}