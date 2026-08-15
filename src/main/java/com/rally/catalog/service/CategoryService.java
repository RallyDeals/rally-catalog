package com.rally.catalog.service;

import com.rally.catalog.dto.CategoryRequest;
import com.rally.catalog.dto.CategoryResponse;
import com.rally.catalog.entity.Category;
import com.rally.catalog.mapper.CatalogMapper;
import com.rally.catalog.repository.CategoryRepository;
import com.rally.catalog.repository.ProductRepository;
import com.rally.common.exceptions.shared.BadRequestException;
import com.rally.common.exceptions.shared.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CatalogMapper catalogMapper;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository,
                           CatalogMapper catalogMapper) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.catalogMapper = catalogMapper;
    }

    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BadRequestException("Category already exists: " + request.getName());
        }
        Category category = new Category(request.getName(), request.getDescription());
        return catalogMapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories(boolean includeProductsCount) {
        List<CategoryResponse> categories = catalogMapper.toCategoryResponses(categoryRepository.findAll());
        if (includeProductsCount) {
            Map<String, Long> counts = productRepository.countProductsGroupedByCategory().stream()
                    .collect(Collectors.toMap(row -> (String) row[0], row -> (Long) row[1]));
            categories.forEach(category ->
                    category.setProductsCount(counts.getOrDefault(category.getId(), 0L).intValue()));
        }
        return categories;
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(String id) {
        return catalogMapper.toCategoryResponse(findCategory(id));
    }

    public CategoryResponse updateCategory(String id, CategoryRequest request) {
        Category category = findCategory(id);
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        return catalogMapper.toCategoryResponse(categoryRepository.save(category));
    }

    public void deleteCategory(String id) {
        Category category = findCategory(id);
        if (productRepository.countByCategoryId(id) > 0) {
            throw new BadRequestException("Cannot delete a category that has products");
        }
        categoryRepository.delete(category);
    }

    private Category findCategory(String id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category", id));
    }
}
