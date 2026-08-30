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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    private static final UUID CAT_1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CAT_2 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository, productRepository, new CatalogMapperImpl());
    }

    @Test
    void createCategory_shouldSaveAndReturnCategory() {
        Category category = new Category("Electronics", "Gadgets");
        category.setId(CAT_1);
        when(categoryRepository.existsByNameIgnoreCase("Electronics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryRequest request = new CategoryRequest();
        request.setName("Electronics");
        request.setDescription("Gadgets");

        CategoryResponse response = categoryService.createCategory(request);

        assertEquals(CAT_1, response.getId());
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
        electronics.setId(CAT_1);
        when(categoryRepository.findAll()).thenReturn(List.of(electronics));

        List<CategoryResponse> result = categoryService.listCategories(false);

        assertEquals(1, result.size());
        assertEquals("Electronics", result.get(0).getName());
        assertEquals(0, result.get(0).getProductsCount());
    }

    @Test
    void listCategories_shouldIncludeProductsCountWhenRequested() {
        Category electronics = new Category("Electronics", "Gadgets");
        electronics.setId(CAT_1);
        Category watches = new Category("Watches", "Timepieces");
        watches.setId(CAT_2);
        when(categoryRepository.findAll()).thenReturn(List.of(electronics, watches));
        List<Object[]> counts = new ArrayList<>();
        counts.add(new Object[] {CAT_1, 5L});
        when(productRepository.countProductsGroupedByCategory()).thenReturn(counts);

        List<CategoryResponse> result = categoryService.listCategories(true);

        CategoryResponse electronicsResponse = result.stream()
                .filter(c -> c.getId().equals(CAT_1)).findFirst().orElseThrow();
        CategoryResponse watchesResponse = result.stream()
                .filter(c -> c.getId().equals(CAT_2)).findFirst().orElseThrow();
        assertEquals(5, electronicsResponse.getProductsCount());
        assertEquals(0, watchesResponse.getProductsCount());
    }

    @Test
    void getCategory_shouldThrowWhenNotFound() {
        UUID missingId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        when(categoryRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> categoryService.getCategory(missingId));
    }

    @Test
    void updateCategory_shouldUpdateFields() {
        Category category = new Category("Old", null);
        category.setId(CAT_1);
        when(categoryRepository.findById(CAT_1)).thenReturn(Optional.of(category));
        when(categoryRepository.save(category)).thenReturn(category);

        CategoryRequest request = new CategoryRequest();
        request.setName("New");
        request.setDescription("Updated");

        CategoryResponse response = categoryService.updateCategory(CAT_1, request);

        assertEquals("New", response.getName());
        assertEquals("Updated", response.getDescription());
    }

    @Test
    void deleteCategory_shouldDeleteWhenNoProducts() {
        Category category = new Category("Electronics", null);
        category.setId(CAT_1);
        when(categoryRepository.findById(CAT_1)).thenReturn(Optional.of(category));
        when(productRepository.countByCategoryId(CAT_1)).thenReturn(0L);

        categoryService.deleteCategory(CAT_1);

        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteCategory_shouldRejectWhenCategoryHasProducts() {
        Category category = new Category("Electronics", null);
        category.setId(CAT_1);
        when(categoryRepository.findById(CAT_1)).thenReturn(Optional.of(category));
        when(productRepository.countByCategoryId(CAT_1)).thenReturn(3L);

        assertThrows(BadRequestException.class, () -> categoryService.deleteCategory(CAT_1));
        verify(categoryRepository, never()).delete(any(Category.class));
    }
}
