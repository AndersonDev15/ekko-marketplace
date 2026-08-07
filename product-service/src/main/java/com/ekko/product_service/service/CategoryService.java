package com.ekko.product_service.service;

import com.ekko.product_service.dto.request.CreateCategoryRequest;
import com.ekko.product_service.dto.request.UpdateCategoryRequest;
import com.ekko.product_service.dto.response.CategoryNodeResponse;
import com.ekko.product_service.dto.response.CategoryResponse;
import com.ekko.product_service.entity.Category;
import com.ekko.product_service.enums.ProductStatus;
import com.ekko.product_service.exception.CategoryDeletionException;
import com.ekko.product_service.exception.CategoryNotFoundException;
import com.ekko.product_service.exception.CyclicCategoryException;
import com.ekko.product_service.exception.DuplicateSlugException;
import com.ekko.product_service.exception.InactiveParentCategoryException;
import com.ekko.product_service.repository.CategoryRepository;
import com.ekko.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsBySlug(request.slug())) {
            throw new DuplicateSlugException();
        }

        Category parent = null;
        if (request.parentId() != null) {
            parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(CategoryNotFoundException::new);
            if (!Boolean.TRUE.equals(parent.getIsActive())) {
                throw new InactiveParentCategoryException();
            }
        }

        Category category = new Category();
        category.setName(request.name());
        category.setSlug(request.slug());
        category.setDescription(request.description());
        category.setImageUrl(request.imageUrl());
        category.setParent(parent);
        category.setIsActive(true);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(UUID id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(CategoryNotFoundException::new);

        if (request.slug() != null
                && categoryRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw new DuplicateSlugException();
        }

        if (request.parentId() != null) {
            Category newParent = categoryRepository.findById(request.parentId())
                    .orElseThrow(CategoryNotFoundException::new);
            if (isAncestor(newParent, id)) {
                throw new CyclicCategoryException();
            }
            if (!Boolean.TRUE.equals(newParent.getIsActive())) {
                throw new InactiveParentCategoryException();
            }
            category.setParent(newParent);
        }

        if (request.name() != null) {
            category.setName(request.name());
        }
        if (request.slug() != null) {
            category.setSlug(request.slug());
        }
        if (request.description() != null) {
            category.setDescription(request.description());
        }
        if (request.imageUrl() != null) {
            category.setImageUrl(request.imageUrl());
        }
        if (request.isActive() != null) {
            category.setIsActive(request.isActive());
        }
        category.setUpdatedAt(LocalDateTime.now());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryNodeResponse> getCategoryTree() {
        List<Category> all = categoryRepository.findAllByIsActiveTrue();
        Map<UUID, List<Category>> byParent = new HashMap<>();
        List<CategoryNodeResponse> roots = new ArrayList<>();

        for (Category category : all) {
            UUID parentId = category.getParent() != null ? category.getParent().getId() : null;
            byParent.computeIfAbsent(parentId, k -> new ArrayList<>()).add(category);
        }

        for (Category category : all) {
            if (category.getParent() == null) {
                roots.add(buildNode(category, byParent));
            }
        }
        return roots;
    }

    @Transactional
    public void softDeleteCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(CategoryNotFoundException::new);

        if (categoryRepository.existsByParentIdAndIsActiveTrue(id)) {
            throw new CategoryDeletionException("Category has active subcategories");
        }

        boolean hasActiveProducts = productRepository
                .existsByCategoryIdAndStatusAndDeletedAtIsNull(id, ProductStatus.ACTIVE);
        if (hasActiveProducts) {
            throw new CategoryDeletionException("Category has active products");
        }

        category.setIsActive(false);
        categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public Set<UUID> getDescendantIds(UUID categoryId) {
        categoryRepository.findById(categoryId)
                .orElseThrow(CategoryNotFoundException::new);

        List<Category> allActive = categoryRepository.findAllByIsActiveTrue();
        Map<UUID, List<Category>> byParent = new HashMap<>();
        for (Category category : allActive) {
            UUID parentId = category.getParent() != null ? category.getParent().getId() : null;
            if (parentId != null) {
                byParent.computeIfAbsent(parentId, k -> new ArrayList<>()).add(category);
            }
        }

        Set<UUID> ids = new HashSet<>();
        collectDescendants(categoryId, byParent, ids);
        return ids;
    }

    private void collectDescendants(UUID categoryId, Map<UUID, List<Category>> byParent, Set<UUID> ids) {
        if (!ids.add(categoryId)) {
            return;
        }
        List<Category> children = byParent.get(categoryId);
        if (children != null) {
            for (Category child : children) {
                collectDescendants(child.getId(), byParent, ids);
            }
        }
    }

    private CategoryNodeResponse buildNode(Category category, Map<UUID, List<Category>> byParent) {
        List<CategoryNodeResponse> children = new ArrayList<>();
        List<Category> childCategories = byParent.get(category.getId());
        if (childCategories != null) {
            for (Category child : childCategories) {
                children.add(buildNode(child, byParent));
            }
        }
        return new CategoryNodeResponse(category.getId(), category.getName(),
                category.getSlug(), children);
    }

    private boolean isAncestor(Category newParent, UUID categoryId) {
        Category current = newParent;
        while (current != null) {
            if (current.getId().equals(categoryId)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(),
                category.getSlug(), category.getIsActive());
    }
}