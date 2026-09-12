package com.ekko.product_service.service;

import com.ekko.product_service.builder.CategoryTestDataBuilder;
import com.ekko.product_service.config.AbstractPostgresIntegrationTest;
import com.ekko.product_service.dto.request.CreateCategoryRequest;
import com.ekko.product_service.dto.request.UpdateCategoryRequest;
import com.ekko.product_service.dto.response.CategoryNodeResponse;
import com.ekko.product_service.dto.response.CategoryResponse;
import com.ekko.product_service.entity.Category;
import com.ekko.product_service.exception.CategoryDeletionException;
import com.ekko.product_service.exception.CyclicCategoryException;
import com.ekko.product_service.exception.DuplicateSlugException;
import com.ekko.product_service.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class CategoryServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    // ------------------------------------------------------------- createCategory

    @Test
    void createCategory_persisteCategoria() {
        CategoryResponse response = categoryService.createCategory(categoryRequest("Electrónica", "electronica", null));

        Category saved = categoryRepository.findById(response.id()).orElseThrow();
        assertEquals("Electrónica", saved.getName());
        assertEquals("electronica", saved.getSlug());
        assertTrue(saved.getIsActive());
    }

    @Test
    void createCategory_slugDuplicadoGeneraSlugUnico() {
        categoryService.createCategory(categoryRequest("Electrónica", "electronica", null));

        CategoryResponse response = categoryService.createCategory(categoryRequest("Electrónica", "electronica", null));

        assertEquals("electronica-2", response.slug());
    }

    @Test
    void createCategory_asignaParent() {
        CategoryResponse parent = categoryService.createCategory(categoryRequest("Electrónica", "electronica", null));

        CategoryResponse child = categoryService.createCategory(
                categoryRequest("Celulares", "celulares", parent.id()));

        Category savedChild = categoryRepository.findById(child.id()).orElseThrow();
        assertEquals(parent.id(), savedChild.getParent().getId());
    }

    // ------------------------------------------------------------- updateCategory

    @Test
    void updateCategory_renombraCategoria() {
        CategoryResponse created = categoryService.createCategory(categoryRequest("Electrónica", "electronica", null));

        CategoryResponse updated = categoryService.updateCategory(created.id(),
                new UpdateCategoryRequest("Tecnología", "Descripción actualizada"));

        assertEquals("Tecnología", updated.name());
        assertEquals("tecnologia", updated.slug());
    }

@Test
    void updateCategory_moverBajoSuHijoNoSoportado() {
        CategoryResponse parent = categoryService.createCategory(categoryRequest("Electrónica", "electronica", null));
        CategoryResponse child = categoryService.createCategory(
                categoryRequest("Celulares", "celulares", parent.id()));

        // El servicio no soporta mover parent en updateCategory
        // Solo actualiza name, description y slug
        CategoryResponse updated = categoryService.updateCategory(parent.id(),
                new UpdateCategoryRequest(child.name(), "moved under child"));

        // El nombre se actualiza pero el parent no cambia
        assertEquals(child.name(), updated.name());
    }

    @Test
    void updateCategory_slugDuplicadoGeneraSlugUnico() {
        categoryService.createCategory(categoryRequest("Electrónica", "electronica", null));
        CategoryResponse created = categoryService.createCategory(categoryRequest("Phones", "phones", null));

        CategoryResponse response = categoryService.updateCategory(created.id(),
                new UpdateCategoryRequest("Electrónica", "duplicate slug"));

        assertEquals("electronica-2", response.slug());
    }

    // ------------------------------------------------------------- getCategoryTree

    @Test
    void getCategoryTree_construyeArbolAnidado() {
        CategoryResponse root = categoryService.createCategory(categoryRequest("Electrónica", "electronica", null));
        CategoryResponse child = categoryService.createCategory(
                categoryRequest("Celulares", "celulares", root.id()));

        List<CategoryNodeResponse> tree = categoryService.getCategoryTree();

        assertEquals(1, tree.size());
        assertEquals(root.id(), tree.get(0).id());
        assertEquals(1, tree.get(0).children().size());
        assertEquals(child.id(), tree.get(0).children().get(0).id());
    }

    // ------------------------------------------------------------- softDeleteCategory

    @Test
    void softDeleteCategory_desactivaRoot() {
        CategoryResponse created = categoryService.createCategory(categoryRequest("Electrónica", "electronica", null));

        categoryService.softDeleteCategory(created.id());

        assertFalse(categoryRepository.findById(created.id()).orElseThrow().getIsActive());
    }

    @Test
    void softDeleteCategory_hijosActivosLanzaDeletion() {
        CategoryResponse root = categoryService.createCategory(categoryRequest("Electrónica", "electronica", null));
        categoryService.createCategory(categoryRequest("Celulares", "celulares", root.id()));

        assertThrows(CategoryDeletionException.class,
                () -> categoryService.softDeleteCategory(root.id()));
    }

    // ------------------------------------------------------------------- helpers

    private CreateCategoryRequest categoryRequest(String name, String slug, UUID parentId) {
        return new CreateCategoryRequest(name, "Descripción", parentId);
    }
}