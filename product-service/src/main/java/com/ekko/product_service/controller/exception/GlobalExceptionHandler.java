package com.ekko.product_service.controller.exception;

import com.ekko.product_service.dto.response.ErrorResponse;
import com.ekko.product_service.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException ex) {
        ErrorResponse body = new ErrorResponse(
                "INSUFFICIENT_STOCK",
                ex.getMessage(),
                ex.getVariantId(),
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Void> handleProductNotFound(ProductNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @ExceptionHandler(VariantNotFoundException.class)
    public ResponseEntity<Void> handleVariantNotFound(VariantNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @ExceptionHandler(ImageNotFoundException.class)
    public ResponseEntity<Void> handleImageNotFound(ImageNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @ExceptionHandler(InventoryNotFoundException.class)
    public ResponseEntity<Void> handleInventoryNotFound(InventoryNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<Void> handleCategoryNotFound(CategoryNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @ExceptionHandler(BrandNotFoundException.class)
    public ResponseEntity<Void> handleBrandNotFound(BrandNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @ExceptionHandler(InvalidImageOrderException.class)
    public ResponseEntity<Void> handleInvalidImageOrder(InvalidImageOrderException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    @ExceptionHandler(InvalidStockAdjustmentException.class)
    public ResponseEntity<Void> handleInvalidStockAdjustment(InvalidStockAdjustmentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    @ExceptionHandler(InvalidPriceRangeException.class)
    public ResponseEntity<Void> handleInvalidPriceRange(InvalidPriceRangeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Void> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    @ExceptionHandler(InvalidStockOperationException.class)
    public ResponseEntity<Void> handleInvalidStockOperation(InvalidStockOperationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @ExceptionHandler(ForbiddenProductAccessException.class)
    public ResponseEntity<Void> handleForbiddenProductAccess(ForbiddenProductAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @ExceptionHandler(InventoryOwnershipException.class)
    public ResponseEntity<Void> handleInventoryOwnership(InventoryOwnershipException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @ExceptionHandler(InvalidDiscountPriceException.class)
    public ResponseEntity<Void> handleInvalidDiscountPrice(InvalidDiscountPriceException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    @ExceptionHandler({
            InvalidProductStatusException.class,
            ProductAlreadyDeletedException.class,
            NoActiveVariantException.class,
            NoPrimaryImageException.class,
            DuplicateSkuException.class,
            LastActiveVariantException.class,
            DuplicateSlugException.class,
            InactiveParentCategoryException.class,
            CyclicCategoryException.class,
            CategoryDeletionException.class,
            ProductNotAvailableException.class,
            DuplicateBrandNameException.class,
            DuplicateBrandSlugException.class
    })
    public ResponseEntity<Void> handleConflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
}
