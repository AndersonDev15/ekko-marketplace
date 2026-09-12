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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
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

    @Mock
    private CloudinaryService cloudinaryService;

    @Spy
    private SlugService slugService = new SlugService(productRepository);

    @InjectMocks
    private CategoryService categoryService;

    private final UUID categoryId = UUID.randomUUID();
    private final UUID parentId = UUID.randomUUID();

    // ------------------------------------------------------------- createCategory

    @Test
    void createCategory_creaCategoriaActivaSinParent() {
        when(categoryRepository.existsBySlug("electronica")).thenReturn(false);
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse response = categoryService.createCategory(createRequestWithoutParent("electronica"));

        verify(categoryRepository).save(any());
        assertTrue(response.isActive());
        assertEquals("electronica", response.slug());
    }

    @Test
    void createCategory_slugDuplicadoGeneraSlugUnico() {
        when(categoryRepository.existsBySlug("electronica")).thenReturn(true, false);
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse response = categoryService.createCategory(createRequestWithoutParent("electronica"));

        verify(categoryRepository).save(any());
        assertTrue(response.isActive());
        assertEquals("electronica-2", response.slug());
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
                new UpdateCategoryRequest("Celulares", "celulares"));

        assertEquals("Celulares", response.name());
        assertEquals("celulares", response.slug());
    }

    @Test
    void updateCategory_noExisteLanzaNotFound() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> categoryService.updateCategory(categoryId, new UpdateCategoryRequest(null, null)));
    }

    @Test
    void updateCategory_slugDuplicadoGeneraSlugUnico() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(activeCategory(categoryId)));
        when(categoryRepository.existsBySlugAndIdNot("electronica", categoryId)).thenReturn(true, false);
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse response = categoryService.updateCategory(categoryId,
                new UpdateCategoryRequest("Electrónica", "desc"));

        assertEquals("Electrónica", response.name());
        assertEquals("electronica-2", response.slug());
    }

    @Test
    void updateCategory_cambioNombreGeneraSlugUnico() {
        Category category = activeCategory(categoryId);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsBySlugAndIdNot("electronica", categoryId)).thenReturn(true, false);
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse response = categoryService.updateCategory(categoryId,
                new UpdateCategoryRequest("Electrónica", "desc"));

        assertEquals("Electrónica", response.name());
        assertEquals("electronica-2", response.slug());
    }

    // Tests for parent movement are not applicable since updateCategory only supports name/description changes

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
        return new CreateCategoryRequest("Electrónica", "Descripción", parentId);
    }

    private CreateCategoryRequest createRequestWithoutParent(String slug) {
        return new CreateCategoryRequest("Electrónica", "Descripción", null);
    }

    private Category activeCategory(Category parent) {
        return CategoryTestDataBuilder.aCategory().withId(UUID.randomUUID()).withParent(parent).build();
    }

    private Category activeCategory(UUID id) {
        return CategoryTestDataBuilder.aCategory().withId(id).build();
    }
}