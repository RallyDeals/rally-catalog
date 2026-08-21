package com.rally.catalog.service;

import com.rally.catalog.dto.DealProductResponse;
import com.rally.catalog.dto.ProductLookupItem;
import com.rally.catalog.dto.ProductLookupResponse;
import com.rally.catalog.entity.Product;
import com.rally.catalog.mapper.CatalogMapper;
import com.rally.catalog.repository.ProductRepository;
import com.rally.catalog.repository.ProductSpecifications;
import com.rally.common.exceptions.domain.catalog.ProductNotFoundException;
import com.rally.common.exceptions.shared.BadRequestException;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InternalProductService {
    private final CatalogMapper catalogMapper;
    private final ProductRepository productRepository;


    @Transactional
    public DealProductResponse getDealProductResponse(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return catalogMapper.toDealProductResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductLookupResponse lookupProducts(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("productIds must not be empty");
        }
        if (ids.size() > 50) {
            throw new BadRequestException("At most 50 product IDs per lookup");
        }

        Set<String> uniqueIds = new LinkedHashSet<>(ids);
        Specification<Product> spec = ProductSpecifications.idIn(uniqueIds)
                .and(ProductSpecifications.approvedAndNotDeleted());
        List<Product> found = productRepository.findAll(spec);
        Map<String, ProductLookupItem> foundMap = found.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        catalogMapper::toProductLookupItem));

        ProductLookupResponse response = new ProductLookupResponse();
        response.setFound(foundMap);
        response.setNotFound(uniqueIds.stream()
                .filter(id -> !foundMap.containsKey(id))
                .collect(Collectors.toList()));
        return response;
    }
}
