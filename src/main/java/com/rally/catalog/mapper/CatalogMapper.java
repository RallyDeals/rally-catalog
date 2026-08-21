package com.rally.catalog.mapper;

import com.rally.catalog.dto.*;
import com.rally.catalog.entity.Category;
import com.rally.catalog.entity.Product;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CatalogMapper {

    ProductResponse toProductResponse(Product product);

    List<ProductResponse> toProductResponses(List<Product> products);

    @Mapping(target = "productName", source = "name")
    @Mapping(target = "productImageUrl", source = "imageUrl")
    DealProductResponse toDealProductResponse(Product product);

    CategoryResponse toCategoryResponse(Category category);

    List<CategoryResponse> toCategoryResponses(List<Category> categories);

    default ProductLookupItem toProductLookupItem(Product product) {
        ProductLookupItem item = new ProductLookupItem();
        item.setId(product.getId());
        item.setName(product.getName());
        item.setBasePrice(product.getBasePrice());
        item.setImageUrl(firstImage(product));
        item.setSellerId(product.getSellerId());
        return item;
    }

    default String firstImage(Product product) {
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            return product.getImages().get(0);
        }
        return product.getImageUrl();
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sellerId", ignore = true)
    @Mapping(target = "sellerName", ignore = true)
    @Mapping(target = "category", ignore = true) // resolved separately from categoryId
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void applyUpdate(@MappingTarget Product product, ProductUpdateRequest request);
}
