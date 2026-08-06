package com.ekko.product_service.service;

import com.ekko.product_service.dto.response.ProductCatalogResponse;
import com.ekko.product_service.dto.response.ProductDetailResponse;
import com.ekko.product_service.entity.Product;
import com.ekko.product_service.enums.ProductStatus;
import com.ekko.product_service.exception.ProductNotFoundException;
import com.ekko.product_service.mapper.ProductMapper;
import com.ekko.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublicProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public Page<ProductCatalogResponse> getPublicCatalog(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository
                .findByStatusAndDeletedAtIsNull(ProductStatus.ACTIVE, pageable)
                .map(productMapper::toCatalogResponse);
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getPublicProduct(String sellerSlug, String productSlug) {
        Product product = productRepository
                .findBySellerSlugAndSlugAndStatusAndDeletedAtIsNull(
                        sellerSlug, productSlug, ProductStatus.ACTIVE)
                .orElseThrow(ProductNotFoundException::new);
        return productMapper.toDetailResponse(product);
    }
}