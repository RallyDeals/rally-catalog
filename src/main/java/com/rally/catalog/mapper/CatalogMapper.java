package com.rally.catalog.mapper;

import com.rally.catalog.dto.CategoryResponse;
import com.rally.catalog.dto.ProductLookupItem;
import com.rally.catalog.dto.ProductResponse;
import com.rally.catalog.entity.Category;
import com.rally.catalog.entity.Product;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CatalogMapper {

    ProductResponse toProductResponse(Product product);

    List<ProductResponse> toProductResponses(List<Product> products);

    CategoryResponse toCategoryResponse(Category category);

    List<CategoryResponse> toCategoryResponses(List<Category> categories);

    ProductLookupItem toProductLookupItem(Product product);
}
