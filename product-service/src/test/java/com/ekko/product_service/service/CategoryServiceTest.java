package com.ekko.product_service.service;

import com.ekko.product_service.builder.CategoryTestDataBuilder;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    private CategoryService categoryService;

    private final UUID categoryId = UUID.randomUUID();
    private final UUID parentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository, productRepository);
    }

    // ------------------------------------------------------------- createCategory

    @Test
    void createCategory_creaCategoriaActivaSinParent() {
        when(categoryRepository.existsBySlug("electronica")).thenReturn(false);
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse response = categoryService.createCategory(createRequestWithoutParent("electronica"));

        verify(categoryRepository).save(any());
        assertTrue(response.isActive());
    }

    @Test
    void createCategory_slugDuplicadoLanzaDuplicateSlug() {
        when(categoryRepository.existsBySlug("electronica")).thenReturn(true);

        assertThrows(DuplicateSlugException.class,
                () -> categoryService.createCategory(createRequest("electronica")));
    }

    @Test
    void createCategory_parentNoExisteLanzaNotFound() {
        when(categoryRepository.existsBySlug("electronica")).thenReturn(false);
        when(categoryRepository.findById(parentId)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> categoryService.createCategory(createRequest("electronica")));
    }

    @Test
    void createCategory_parentInactivoLanzaInactiveParent() {
        Category parent = CategoryTestDataBuilder.aCategory().withId(parentId).withIsActive(false).build();
        when(categoryRepository.existsBySlug("electronica")).thenReturn(false);
        when(categoryRepository.findById(parentId)).thenReturn(Optional.of(parent));

        assertThrows(InactiveParentCategoryException.class,
                () -> categoryService.createCategory(createRequest("electronica")));
    }

    @Test
    void createCategory_parentActivoQuedaAsignado() {
        Category parent = activeCategory(parentId);
        when(categoryRepository.existsBySlug("electronica")).thenReturn(false);
        when(categoryRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        categoryService.createCategory(createRequest("electronica"));

        verify(categoryRepository).save(any());
    }

    // ------------------------------------------------------------- updateCategory

    @Test
    void updateCategory_actualizaCampos() {
        Category category = activeCategory(categoryId);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsBySlugAndIdNot("celulares", categoryId)).thenReturn(false);
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse response = categoryService.updateCategory(categoryId,
                new UpdateCategoryRequest("Celulares", "celulares", null, null, null, null));

        assertEquals("Celulares", response.name());
        assertEquals("celulares", response.slug());
    }

    @Test
    void updateCategory_noExisteLanzaNotFound() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> categoryService.updateCategory(categoryId, new UpdateCategoryRequest(null, null, null, null, null, null)));
    }

    @Test
    void updateCategory_slugDuplicadoLanzaDuplicateSlug() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(activeCategory(categoryId)));
        when(categoryRepository.existsBySlugAndIdNot("dup", categoryId)).thenReturn(true);

        assertThrows(DuplicateSlugException.class, () -> categoryService.updateCategory(categoryId,
                new UpdateCategoryRequest(null, "dup", null, null, null, null)));
    }

    @Test
    void updateCategory_moverBajoSuHijoLanzaCyclic() {
        Category self = activeCategory(categoryId);
        Category child = activeCategory(UUID.randomUUID());
        child.setParent(self);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(self));
        when(categoryRepository.findById(child.getId())).thenReturn(Optional.of(child));

        assertThrows(CyclicCategoryException.class, () -> categoryService.updateCategory(categoryId,
                new UpdateCategoryRequest(null, null, null, null, child.getId(), null)));
    }

    @Test
    void updateCategory_moverseASiMismoLanzaCyclic() {
        Category self = activeCategory(categoryId);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(self));

        assertThrows(CyclicCategoryException.class, () -> categoryService.updateCategory(categoryId,
                new UpdateCategoryRequest(null, null, null, null, categoryId, null)));
    }

    @Test
    void updateCategory_parentInactivoLanzaInactiveParent() {
        Category self = activeCategory(categoryId);
        Category parent = activeCategory(parentId);
        parent.setIsActive(false);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(self));
        when(categoryRepository.findById(parentId)).thenReturn(Optional.of(parent));

        assertThrows(InactiveParentCategoryException.class, () -> categoryService.updateCategory(categoryId,
                new UpdateCategoryRequest(null, null, null, null, parentId, null)));
    }

    // ------------------------------------------------------------- getCategoryTree

    void getCategoryTree_devuelveSoloActivasEnArbol() {
        Category root = activeCategory(parentId);
        Category child = activeCategory(categoryId);
        child.setParent(root);

        when(categoryRepository.findAllByIsActiveTrue()).thenReturn(List.of(root, child));

        List<CategoryNodeResponse> tree = categoryService.getCategoryTree();

        assertEquals(1, tree.size());
        assertEquals(parentId, tree.get(0).id());
        assertEquals(1, tree.get(0).children().size());
        assertEquals(categoryId, tree.get(0).children().get(0).id());
    }

    // ------------------------------------------------------------- softDeleteCategory

    @Test
    void softDeleteCategory_desactivaCategoria() {
        Category category = activeCategory(categoryId);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByParentIdAndIsActiveTrue(categoryId)).thenReturn(false);
        when(productRepository.existsByCategoryIdAndStatusAndDeletedAtIsNull(categoryId, ProductStatus.ACTIVE))
                .thenReturn(false);

        categoryService.softDeleteCategory(categoryId);

        assertFalse(category.getIsActive());
        verify(categoryRepository).save(category);
    }

    @Test
    void softDeleteCategory_noExisteLanzaNotFound() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> categoryService.softDeleteCategory(categoryId));
    }

    @Test
    void softDeleteCategory_haySubcategoriasActivasLanzaDeletion() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(activeCategory(categoryId)));
        when(categoryRepository.existsByParentIdAndIsActiveTrue(categoryId)).thenReturn(true);

        assertThrows(CategoryDeletionException.class, () -> categoryService.softDeleteCategory(categoryId));
    }

    @Test
    void softDeleteCategory_hayProductosActivosLanzaDeletion() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(activeCategory(categoryId)));
        when(categoryRepository.existsByParentIdAndIsActiveTrue(categoryId)).thenReturn(false);
        when(productRepository.existsByCategoryIdAndStatusAndDeletedAtIsNull(categoryId, ProductStatus.ACTIVE))
                .thenReturn(true);

        assertThrows(CategoryDeletionException.class, () -> categoryService.softDeleteCategory(categoryId));
    }

    // ------------------------------------------------------------- helpers

    private CreateCategoryRequest createRequest(String slug) {
        return new CreateCategoryRequest("Electrónica", slug, null, null, parentId);
    }

    private CreateCategoryRequest createRequestWithoutParent(String slug) {
        return new CreateCategoryRequest("Electrónica", slug, null, null, null);
    }

    private Category activeCategory(Category parent) {
        return CategoryTestDataBuilder.aCategory().withId(UUID.randomUUID()).withParent(parent).build();
    }

    private Category activeCategory(UUID id) {
        return CategoryTestDataBuilder.aCategory().withId(id).build();
    }
}