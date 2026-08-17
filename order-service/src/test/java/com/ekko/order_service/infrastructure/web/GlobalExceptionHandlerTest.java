package com.ekko.order_service.infrastructure.web;

import com.ekko.order_service.domain.exception.InsufficientStockException;
import com.ekko.order_service.domain.exception.InvalidGuestEmailException;
import com.ekko.order_service.domain.exception.InvalidOrderStatusTransitionException;
import com.ekko.order_service.application.exception.OrderAccessDeniedException;
import com.ekko.order_service.domain.exception.OrderCancellationNotAllowedException;
import com.ekko.order_service.application.exception.OrderNotFoundException;
import com.ekko.order_service.application.exception.OrderNumberGenerationException;
import com.ekko.order_service.application.exception.StockReservationException;
import com.ekko.order_service.domain.enums.OrderStatus;
import com.ekko.order_service.infrastructure.persistence.adapter.in.web.exception.ErrorResponse;
import com.ekko.order_service.infrastructure.persistence.adapter.in.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private static final UUID VARIANT_ID = UUID.randomUUID();

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("OrderNotFoundException -> 404 ORDER_NOT_FOUND")
    void orderNotFound() {
        ResponseEntity<ErrorResponse> result = handler.handleOrderNotFound(
                new OrderNotFoundException("EKK-1"));

        assertEquals(404, result.getStatusCode().value());
        assertErrorBody(result, "ORDER_NOT_FOUND", null);
    }

    @Test
    @DisplayName("OrderAccessDeniedException -> 403 ORDER_ACCESS_DENIED")
    void orderAccessDenied() {
        ResponseEntity<ErrorResponse> result = handler.handleOrderAccessDenied(
                new OrderAccessDeniedException());

        assertEquals(403, result.getStatusCode().value());
        assertErrorBody(result, "ORDER_ACCESS_DENIED", null);
    }

    @Test
    @DisplayName("OrderCancellationNotAllowedException -> 409 CANCELLATION_NOT_ALLOWED")
    void cancellationNotAllowed() {
        ResponseEntity<ErrorResponse> result = handler.handleOrderCancellationNotAllowed(
                new OrderCancellationNotAllowedException(OrderStatus.SHIPPED));

        assertEquals(409, result.getStatusCode().value());
        assertErrorBody(result, "CANCELLATION_NOT_ALLOWED", null);
    }

    @Test
    @DisplayName("InvalidOrderStatusTransitionException -> 409 INVALID_STATUS_TRANSITION")
    void invalidTransition() {
        ResponseEntity<ErrorResponse> result = handler.handleInvalidOrderStatusTransition(
                new InvalidOrderStatusTransitionException(OrderStatus.CONFIRMED, OrderStatus.PENDING));

        assertEquals(409, result.getStatusCode().value());
        assertErrorBody(result, "INVALID_STATUS_TRANSITION", null);
    }

    @Test
    @DisplayName("InsufficientStockException -> 409 INSUFFICIENT_STOCK con variantId presente")
    void insufficientStockIncludesVariantId() {
        ResponseEntity<ErrorResponse> result = handler.handleInsufficientStock(
                new InsufficientStockException(VARIANT_ID, 5));

        assertEquals(409, result.getStatusCode().value());
        assertErrorBody(result, "INSUFFICIENT_STOCK", VARIANT_ID);
    }

    @Test
    @DisplayName("StockReservationException -> 503 STOCK_SERVICE_UNAVAILABLE")
    void stockReservation() {
        ResponseEntity<ErrorResponse> result = handler.handleStockReservation(
                new StockReservationException("stock service down"));

        assertEquals(503, result.getStatusCode().value());
        assertErrorBody(result, "STOCK_SERVICE_UNAVAILABLE", null);
    }

    @Test
    @DisplayName("InvalidGuestEmailException -> 400 INVALID_GUEST_EMAIL")
    void invalidGuestEmail() {
        ResponseEntity<ErrorResponse> result = handler.handleInvalidGuestEmail(
                new InvalidGuestEmailException("bad email"));

        assertEquals(400, result.getStatusCode().value());
        assertErrorBody(result, "INVALID_GUEST_EMAIL", null);
    }

    @Test
    @DisplayName("OrderNumberGenerationException -> 500 ORDER_NUMBER_GENERATION_FAILED")
    void orderNumberGeneration() {
        ResponseEntity<ErrorResponse> result = handler.handleOrderNumberGeneration(
                new OrderNumberGenerationException("boom"));

        assertEquals(500, result.getStatusCode().value());
        assertErrorBody(result, "ORDER_NUMBER_GENERATION_FAILED", null);
    }

    @Test
    @DisplayName("MethodArgumentNotValidException -> 400 VALIDATION_ERROR con detalles de campos")
    void methodArgumentNotValid() throws Exception {
        BindingResult bindingResult = mock(BindingResult.class);
        org.springframework.validation.FieldError fieldError =
                new org.springframework.validation.FieldError("order",
                        "shippingAddress", "must not be null");
        when(bindingResult.getFieldErrors())
                .thenReturn(java.util.List.of(fieldError));

        ResponseEntity<ErrorResponse> result =
                handler.handleMethodArgumentNotValid(
                        new MethodArgumentNotValidException(null, bindingResult));

        assertEquals(400, result.getStatusCode().value());
        assertEquals("VALIDATION_ERROR", result.getBody().error());
        assertEquals("shippingAddress: must not be null", result.getBody().message());
        assertNull(result.getBody().variantId());
    }

    private void assertErrorBody(ResponseEntity<ErrorResponse> result, String error, UUID variantId) {
        ErrorResponse body = result.getBody();
        assertEquals(error, body.error());
        assertEquals(result.getStatusCode().value(), body.httpStatus());
        assertEquals(variantId, body.variantId());
    }
}