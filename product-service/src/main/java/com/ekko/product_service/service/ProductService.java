package com.ekko.product_service.service;

import com.ekko.product_service.dto.request.CreateProductAttributeRequest;
import com.ekko.product_service.dto.request.CreateProductRequest;
import com.ekko.product_service.dto.request.UpdateProductRequest;
import com.ekko.product_service.dto.response.ProductDetailResponse;
import com.ekko.product_service.dto.response.ProductResponse;
import com.ekko.product_service.entity.Brand;
import com.ekko.product_service.entity.Category;
import com.ekko.product_service.entity.Product;
import com.ekko.product_service.entity.ProductAttribute;
import com.ekko.product_service.entity.ProductImage;
import com.ekko.product_service.entity.ProductVariant;
import com.ekko.product_service.enums.ProductStatus;
import com.ekko.product_service.exception.InvalidProductStatusException;
import com.ekko.product_service.exception.NoActiveVariantException;
import com.ekko.product_service.exception.NoPrimaryImageException;
import com.ekko.product_service.exception.ProductAlreadyDeletedException;
import com.ekko.product_service.exception.ProductNotAvailableException;
import com.ekko.product_service.exception.ProductNotFoundException;
import com.ekko.product_service.exception.VariantNotFoundException;
import com.ekko.product_service.mapper.ProductMapper;
import com.ekko.product_service.repository.BrandRepository;
import com.ekko.product_service.repository.CategoryRepository;
import com.ekko.product_service.repository.ProductAttributeRepository;
import com.ekko.product_service.repository.ProductRepository;
import com.ekko.product_service.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductAttributeRepository productAttributeRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final SlugService slugService;
    private final OwnershipValidator ownershipValidator;
    private final ProductMapper productMapper;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request, UUID sellerKeycloakId) {
        String slug = slugService.generateUnique(request.name(), sellerKeycloakId);

        // TODO: obtener sellerSlug desde seller-service
        String sellerSlug = "pending";

        Product product = Product.builder()
                .sellerKeycloakId(sellerKeycloakId)
                .sellerSlug(sellerSlug)
                .brand(resolveBrand(request.brandId()))
                .category(resolveCategory(request.categoryId()))
                .name(request.name())
                .slug(slug)
                .description(request.description())
                .status(ProductStatus.DRAFT)
                .averageRating(BigDecimal.ZERO)
                .reviewCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        productRepository.save(product);

        if (request.attributes() != null) {
            for (CreateProductAttributeRequest attributeRequest : request.attributes()) {
                ProductAttribute attribute = ProductAttribute.builder()
                        .product(product)
                        .name(attributeRequest.name())
                        .value(attributeRequest.value())
                        .build();
                productAttributeRepository.save(attribute);
                product.getAttributes().add(attribute);
            }
        }

        return productMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(UUID productId, UpdateProductRequest request, UUID sellerKeycloakId) {
        Product product = ownershipValidator.validate(productId, sellerKeycloakId);

        if (product.getDeletedAt() != null) {
            throw new ProductNotFoundException();
        }

        ProductStatus status = product.getStatus();
        if (status != ProductStatus.DRAFT && status != ProductStatus.REJECTED) {
            throw new InvalidProductStatusException();
        }

        if (request.name() != null && !request.name().equals(product.getName())) {
            product.setName(request.name());
            product.setSlug(slugService.generateUnique(request.name(), sellerKeycloakId));
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.brandId() != null) {
            product.setBrand(resolveBrand(request.brandId()));
        }
        if (request.categoryId() != null) {
            product.setCategory(resolveCategory(request.categoryId()));
        }
        product.setUpdatedAt(LocalDateTime.now());

        productRepository.save(product);

        return productMapper.toResponse(product);
    }

    @Transactional
    public void submitForReview(UUID productId, UUID sellerKeycloakId) {
        Product product = ownershipValidator.validate(productId, sellerKeycloakId);

        if (product.getDeletedAt() != null) {
            throw new ProductNotFoundException();
        }

        ProductStatus status = product.getStatus();
        if (status != ProductStatus.DRAFT && status != ProductStatus.REJECTED) {
            throw new InvalidProductStatusException();
        }

        boolean hasActiveVariant = product.getVariants().stream()
                .anyMatch(ProductVariant::getIsActive);
        if (!hasActiveVariant) {
            throw new NoActiveVariantException();
        }

        boolean hasPrimaryImage = product.getImages().stream()
                .anyMatch(ProductImage::getIsPrimary);
        if (!hasPrimaryImage) {
            throw new NoPrimaryImageException();
        }

        product.setStatus(ProductStatus.PENDING_REVIEW);
        product.setUpdatedAt(LocalDateTime.now());

        productRepository.save(product);
    }

    @Transactional
    public void softDeleteProduct(UUID productId, UUID sellerKeycloakId) {
        Product product = ownershipValidator.validate(productId, sellerKeycloakId);

        if (product.getDeletedAt() != null) {
            throw new ProductAlreadyDeletedException();
        }

        ProductStatus previousStatus = product.getStatus();

        product.setDeletedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        List<ProductVariant> variants = product.getVariants();
        if (variants != null) {
            variants.forEach(variant -> variant.setIsActive(false));
        }

        productRepository.save(product);

        if (previousStatus == ProductStatus.ACTIVE) {
            // TODO: publicar ProductDeactivatedEvent
        }
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getMyProducts(UUID sellerKeycloakId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository
                .findBySellerKeycloakIdAndDeletedAtIsNull(sellerKeycloakId, pageable)
                .map(productMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getProductById(UUID productId, UUID sellerKeycloakId) {
        Product product = ownershipValidator.validate(productId, sellerKeycloakId);
        return productMapper.toDetailResponse(product);
    }

    @Transactional(readOnly = true)
    public void verifyPurchasable(UUID variantId) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(VariantNotFoundException::new);

        if (variant.getProduct().getStatus() != ProductStatus.ACTIVE) {
            throw new ProductNotAvailableException();
        }
    }

    private Brand resolveBrand(UUID brandId) {
        if (brandId == null) {
            return null;
        }
        return brandRepository.findById(brandId)
                .orElseThrow(ProductNotFoundException::new);
    }

    private Category resolveCategory(UUID categoryId) {
        if (categoryId == null) {
            throw new ProductNotFoundException();
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(ProductNotFoundException::new);
    }
}