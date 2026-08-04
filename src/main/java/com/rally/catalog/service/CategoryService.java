package com.rally.catalog.service;

import com.rally.catalog.dto.CategoryRequest;
import com.rally.catalog.dto.CategoryResponse;
import com.rally.catalog.entity.Category;
import com.rally.catalog.repository.CategoryRepository;
import com.rally.catalog.repository.ProductRepository;
import com.rally.common.exceptions.shared.BadRequestException;
import com.rally.common.exceptions.shared.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BadRequestException("Category already exists: " + request.getName());
        }
        Category category = new Category(request.getName(), request.getDescription());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(String id) {
        return CategoryResponse.from(findCategory(id));
    }

    public CategoryResponse updateCategory(String id, CategoryRequest request) {
        Category category = findCategory(id);
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        return CategoryResponse.from(categoryRepository.save(category));
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
