package com.rally.catalog.service;

import com.rally.catalog.dto.CategoryRequest;
import com.rally.catalog.dto.CategoryResponse;
import com.rally.catalog.entity.Category;
import com.rally.catalog.mapper.CatalogMapperImpl;
import com.rally.catalog.repository.CategoryRepository;
import com.rally.catalog.repository.ProductRepository;
import com.rally.common.exceptions.shared.BadRequestException;
import com.rally.common.exceptions.shared.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository, productRepository, new CatalogMapperImpl());
    }

    @Test
    void createCategory_shouldSaveAndReturnCategory() {
        Category category = new Category("Electronics", "Gadgets");
        category.setId("cat-1");
        when(categoryRepository.existsByNameIgnoreCase("Electronics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryRequest request = new CategoryRequest();
        request.setName("Electronics");
        request.setDescription("Gadgets");

        CategoryResponse response = categoryService.createCategory(request);

        assertEquals("cat-1", response.getId());
        assertEquals("Electronics", response.getName());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategory_shouldRejectDuplicateName() {
        when(categoryRepository.existsByNameIgnoreCase("Electronics")).thenReturn(true);

        CategoryRequest request = new CategoryRequest();
        request.setName("Electronics");

        assertThrows(BadRequestException.class, () -> categoryService.createCategory(request));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void listCategories_shouldReturnAllCategories() {
        Category electronics = new Category("Electronics", "Gadgets");
        electronics.setId("cat-1");
        when(categoryRepository.findAll()).thenReturn(List.of(electronics));

        List<CategoryResponse> result = categoryService.listCategories();

        assertEquals(1, result.size());
        assertEquals("Electronics", result.get(0).getName());
    }

    @Test
    void getCategory_shouldThrowWhenNotFound() {
        when(categoryRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> categoryService.getCategory("missing"));
    }

    @Test
    void updateCategory_shouldUpdateFields() {
        Category category = new Category("Old", null);
        category.setId("cat-1");
        when(categoryRepository.findById("cat-1")).thenReturn(Optional.of(category));
        when(categoryRepository.save(category)).thenReturn(category);

        CategoryRequest request = new CategoryRequest();
        request.setName("New");
        request.setDescription("Updated");

        CategoryResponse response = categoryService.updateCategory("cat-1", request);

        assertEquals("New", response.getName());
        assertEquals("Updated", response.getDescription());
    }

    @Test
    void deleteCategory_shouldDeleteWhenNoProducts() {
        Category category = new Category("Electronics", null);
        category.setId("cat-1");
        when(categoryRepository.findById("cat-1")).thenReturn(Optional.of(category));
        when(productRepository.countByCategoryId("cat-1")).thenReturn(0L);

        categoryService.deleteCategory("cat-1");

        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteCategory_shouldRejectWhenCategoryHasProducts() {
        Category category = new Category("Electronics", null);
        category.setId("cat-1");
        when(categoryRepository.findById("cat-1")).thenReturn(Optional.of(category));
        when(productRepository.countByCategoryId("cat-1")).thenReturn(3L);

        assertThrows(BadRequestException.class, () -> categoryService.deleteCategory("cat-1"));
        verify(categoryRepository, never()).delete(any(Category.class));
    }
}
