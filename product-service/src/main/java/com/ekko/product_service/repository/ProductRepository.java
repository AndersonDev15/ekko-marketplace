package com.ekko.product_service.repository;

import com.ekko.product_service.entity.Product;
import com.ekko.product_service.enums.ProductStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository
        extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    boolean existsBySellerKeycloakIdAndSlug(UUID sellerKeycloakId, String slug);
    boolean existsBySellerKeycloakIdAndSlugAndIdNot(
            UUID sellerKeycloakId,
            String slug,
            UUID productId
    );

    Page<Product> findBySellerKeycloakIdAndDeletedAtIsNull(UUID sellerKeycloakId, Pageable pageable);

    Optional<Product> findBySellerSlugAndSlugAndStatusAndDeletedAtIsNull(
            String sellerSlug,
            String productSlug,
            ProductStatus status);

    Page<Product> findByStatusAndDeletedAtIsNull(ProductStatus status, Pageable pageable);

    Page<Product> findByDeletedAtIsNull(Pageable pageable);

    Optional<Product> findBySlugAndStatusAndDeletedAtIsNull(String slug, ProductStatus status);

    boolean existsByCategoryIdAndStatusAndDeletedAtIsNull(UUID categoryId, ProductStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :productId")
    Optional<Product> findByIdForUpdate(@Param("productId") UUID productId);

    @Override
    @EntityGraph(attributePaths = {"variants.inventory", "brand", "category"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);


    @Modifying
    @Query("""
    update Product p
    set p.sellerSlug = :sellerSlug
    where p.sellerKeycloakId = :sellerKeycloakId
""")
    int updateSellerSlugBySellerKeycloakId(
            @Param("sellerKeycloakId") UUID sellerKeycloakId,
            @Param("sellerSlug") String sellerSlug
    );
}