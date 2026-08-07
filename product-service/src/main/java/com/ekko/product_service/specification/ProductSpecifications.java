package com.ekko.product_service.specification;

import com.ekko.product_service.entity.Product;
import com.ekko.product_service.entity.ProductVariant;
import com.ekko.product_service.enums.ProductStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.UUID;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.equal(root.get("status"), ProductStatus.ACTIVE);
    }

    public static Specification<Product> isNotDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    /**
     * EXISTS (SELECT 1 FROM ProductVariant pv JOIN pv.inventory inv
     *          WHERE pv.product = product AND pv.isActive = TRUE
     *            AND inv.stockAvailable - inv.stockReserved > 0)
     */
    public static Specification<Product> hasSellableStock() {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<ProductVariant> variant = sub.from(ProductVariant.class);
            Join<ProductVariant, Object> inventory = variant.join("inventory");
            sub.select(cb.literal(1L));
            sub.where(
                    cb.equal(variant.get("product"), root),
                    cb.equal(variant.get("isActive"), true),
                    cb.greaterThan(
                            cb.diff(inventory.get("stockAvailable"), inventory.get("stockReserved")),
                            0L));
            return cb.exists(sub);
        };
    }

    public static Specification<Product> hasCategoryIn(Collection<UUID> categoryIds) {
        return (root, query, cb) -> root.get("category").get("id").in(categoryIds);
    }

    public static Specification<Product> hasBrand(UUID brandId) {
        return (root, query, cb) -> cb.equal(root.get("brand").get("id"), brandId);
    }

    public static Specification<Product> hasSeller(UUID sellerId) {
        return (root, query, cb) -> cb.equal(root.get("sellerKeycloakId"), sellerId);
    }

    public static Specification<Product> matchesQuery(String query) {
        return (root, criteriaQuery, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + query.toLowerCase() + "%");
    }

    /**
     * El filtro de precio opera sobre el precio MÍNIMO de las variantes del
     * producto, no sobre un campo price inexistente en Product. Usa el mismo
     * subquery correlacionado MIN(variant.price).
     */
    public static Specification<Product> priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            Subquery<BigDecimal> minPriceSub = query.subquery(BigDecimal.class);
            Root<ProductVariant> variant = minPriceSub.from(ProductVariant.class);
            minPriceSub.select(cb.min(variant.get("price")));
            minPriceSub.where(
                    cb.equal(variant.get("product"), root),
                    cb.equal(variant.get("isActive"), true));

            Predicate predicate = cb.conjunction();
            if (minPrice != null) {
                predicate = cb.and(predicate,
                        cb.greaterThanOrEqualTo(minPriceSub, cb.literal(minPrice)));
            }
            if (maxPrice != null) {
                predicate = cb.and(predicate,
                        cb.lessThanOrEqualTo(minPriceSub, cb.literal(maxPrice)));
            }
            return predicate;
        };
    }

    /**
     * Permite ordenar por el precio mínimo SIN usar el Sort del Pageable, ya
     * que Spring Data no puede generar un ORDER BY sobre un subquery. La
     * specification fija query.orderBy(...) con la subquery MIN(variant.price).
     * Se combina con un Pageable sin sort.
     */
    public static Specification<Product> sortedByMinPrice(boolean ascending) {
        return (root, query, cb) -> {
            Subquery<BigDecimal> minPriceSub = query.subquery(BigDecimal.class);
            Root<ProductVariant> variant = minPriceSub.from(ProductVariant.class);
            minPriceSub.select(cb.min(variant.get("price")));
            minPriceSub.where(
                    cb.equal(variant.get("product"), root),
                    cb.equal(variant.get("isActive"), true));

            Order order = ascending ? cb.asc(minPriceSub) : cb.desc(minPriceSub);
            query.orderBy(order);
            return cb.conjunction();
        };
    }
}