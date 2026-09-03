package com.ekko.product_service.service;

import com.ekko.product_service.dto.response.ProductResponse;
import com.ekko.product_service.entity.Product;
import com.ekko.product_service.enums.ProductStatus;
import com.ekko.product_service.exception.InvalidProductStatusException;
import com.ekko.product_service.exception.ProductNotFoundException;
import com.ekko.product_service.mapper.ProductMapper;
import com.ekko.product_service.messaging.ProductEventPublisher;
import com.ekko.product_service.messaging.dto.publish.ProductPublishedEvent;
import com.ekko.product_service.messaging.dto.publish.ProductRejectedEvent;
import com.ekko.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductEventPublisher productEventPublisher;

    @Transactional
    public void approveProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        if (product.getStatus() != ProductStatus.PENDING_REVIEW) {
            throw new InvalidProductStatusException();
        }

        product.setStatus(ProductStatus.ACTIVE);
        product.setUpdatedAt(LocalDateTime.now());

        productRepository.save(product);

        productEventPublisher.publishProductPublished(toPublishedEvent(product));
    }

    @Transactional
    public void rejectProduct(UUID productId, String reason) {
        Product product = productRepository.findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        if (product.getStatus() != ProductStatus.PENDING_REVIEW) {
            throw new InvalidProductStatusException();
        }

        product.setStatus(ProductStatus.REJECTED);
        product.setUpdatedAt(LocalDateTime.now());

        productRepository.save(product);

        productEventPublisher.publishProductRejected(toRejectedEvent(product, reason));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(
            ProductStatus status,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Product> products;

        if (status != null) {
            products = productRepository
                    .findByStatusAndDeletedAtIsNull(status, pageable);
        } else {
            products = productRepository
                    .findByDeletedAtIsNull(pageable);
        }

        return products.map(productMapper::toResponse);
    }

    private ProductPublishedEvent toPublishedEvent(Product product) {
        return new ProductPublishedEvent(
                product.getId(),
                product.getSellerKeycloakId(),
                product.getName(),
                product.getCategory().getName(),
                LocalDateTime.now()
        );
    }

    private ProductRejectedEvent toRejectedEvent(Product product, String reason) {
        return new ProductRejectedEvent(
                product.getId(),
                product.getSellerKeycloakId(),
                product.getName(),
                reason,
                LocalDateTime.now()
        );
    }
}