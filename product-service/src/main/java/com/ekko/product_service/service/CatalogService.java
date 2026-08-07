package com.ekko.product_service.service;

import com.ekko.product_service.dto.request.ProductFiltersRequest;
import com.ekko.product_service.dto.response.ProductDetailResponse;
import com.ekko.product_service.dto.response.ProductSummaryResponse;
import com.ekko.product_service.entity.Inventory;
import com.ekko.product_service.entity.Product;
import com.ekko.product_service.entity.ProductImage;
import com.ekko.product_service.entity.ProductVariant;
import com.ekko.product_service.enums.ProductSortOption;
import com.ekko.product_service.enums.ProductStatus;
import com.ekko.product_service.exception.ProductNotFoundException;
import com.ekko.product_service.mapper.ProductMapper;
import com.ekko.product_service.repository.ProductRepository;
import com.ekko.product_service.specification.ProductSpecifications;
import com.ekko.product_service.util.StockCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CategoryService categoryService;

    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> searchProducts(ProductFiltersRequest filters, int page, int size,
                                                       ProductSortOption sort) {
        Specification<Product> spec = baseSpecification(filters);

        Pageable pageable = buildPageable(page, size, sort);
        // para el orden por precio mínimo se necesita un Pageable sin sort y
        // aplicar la ordenación dentro de la specification con la subquery.
        if (sort == ProductSortOption.PRICE_ASC || sort == ProductSortOption.PRICE_DESC) {
            pageable = PageRequest.of(page, size);
            spec = spec.and(ProductSpecifications.sortedByMinPrice(
                    sort == ProductSortOption.PRICE_ASC));
        }

        Page<Product> products = productRepository.findAll(spec, pageable);
        return products.map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(String slug) {
        Product product = productRepository
                .findBySlugAndStatusAndDeletedAtIsNull(slug, ProductStatus.ACTIVE)
                .orElseThrow(ProductNotFoundException::new);
        return productMapper.toDetailResponse(product);
    }

    private Specification<Product> baseSpecification(ProductFiltersRequest filters) {
        Specification<Product> spec = ProductSpecifications.isActive()
                .and(ProductSpecifications.isNotDeleted())
                .and(ProductSpecifications.hasSellableStock());

        if (filters != null && filters.categoryId() != null) {
            Set<UUID> categoryIds = categoryService.getDescendantIds(filters.categoryId());
            spec = spec.and(ProductSpecifications.hasCategoryIn(categoryIds));
        }
        if (filters != null && filters.brandId() != null) {
            spec = spec.and(ProductSpecifications.hasBrand(filters.brandId()));
        }
        if (filters != null && filters.sellerId() != null) {
            spec = spec.and(ProductSpecifications.hasSeller(filters.sellerId()));
        }
        if (filters != null && (filters.minPrice() != null || filters.maxPrice() != null)) {
            spec = spec.and(
                    ProductSpecifications.priceBetween(filters.minPrice(), filters.maxPrice()));
        }
        if (filters != null && filters.query() != null && !filters.query().isBlank()) {
            spec = spec.and(ProductSpecifications.matchesQuery(filters.query()));
        }
        return spec;
    }

    private Pageable buildPageable(int page, int size, ProductSortOption sort) {
        return switch (sort == null ? ProductSortOption.RECENT : sort) {
            case RECENT -> PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            case NAME -> PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
            case PRICE_ASC, PRICE_DESC -> PageRequest.of(page, size);
        };
    }

    private ProductSummaryResponse toSummary(Product product) {
        BigDecimal price = product.getVariants().stream()
                .filter(ProductVariant::getIsActive)
                .map(ProductVariant::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(null);

        String mainImageUrl = product.getImages().stream()
                .filter(ProductImage::getIsPrimary)
                .map(ProductImage::getUrl)
                .findFirst()
                // fallback: primera imagen por orden de inserción (sort_order)
                .or(() -> product.getImages().stream()
                        .min((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                        .map(ProductImage::getUrl))
                .orElse(null);

        String brandName = product.getBrand() != null ? product.getBrand().getName() : null;
        String categoryName = product.getCategory() != null ? product.getCategory().getName() : null;

        boolean hasStock = product.getVariants().stream()
                .filter(ProductVariant::getIsActive)
                .map(ProductVariant::getInventory)
                .filter(java.util.Objects::nonNull)
                .anyMatch(inv -> StockCalculator.getAvailableForSale(inv) > 0);

        return new ProductSummaryResponse(
                product.getId(),
                product.getName(),
                product.getSlug(),
                price,
                mainImageUrl,
                brandName,
                categoryName,
                hasStock);
    }
}