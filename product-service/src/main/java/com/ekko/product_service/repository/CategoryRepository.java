package com.ekko.product_service.repository;

import com.ekko.product_service.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    List<Category> findAllByIsActiveTrue();

    boolean existsByParentIdAndIsActiveTrue(UUID parentId);
}