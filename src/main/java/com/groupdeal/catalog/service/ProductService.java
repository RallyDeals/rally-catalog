package com.groupdeal.catalog.service;

import com.groupdeal.catalog.dto.PageResponse;
import com.groupdeal.catalog.dto.ProductLookupItem;
import com.groupdeal.catalog.dto.ProductLookupResponse;
import com.groupdeal.catalog.dto.ProductRequest;
import com.groupdeal.catalog.dto.ProductResponse;
import com.groupdeal.catalog.dto.ProductUpdateRequest;
import com.groupdeal.catalog.entity.Category;
import com.groupdeal.catalog.entity.Product;
import com.groupdeal.catalog.entity.ProductStatus;
import com.groupdeal.catalog.exception.BadRequestException;
import com.groupdeal.catalog.exception.ForbiddenException;
import com.groupdeal.catalog.exception.GoneException;
import com.groupdeal.catalog.exception.NotFoundException;
import com.groupdeal.catalog.repository.CategoryRepository;
import com.groupdeal.catalog.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_SELLER = "SELLER";
    private static final Set<String> SORTABLE_FIELDS = Set.of("createdAt", "basePrice", "name");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public ProductResponse createProduct(String sellerId, ProductRequest request) {
        Category category = findCategory(request.getCategoryId());
        Product product = new Product(
                sellerId,
                request.getName(),
                request.getDescription(),
                category,
                request.getBasePrice(),
                request.getImageUrl()
        );
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProducts(
            String q, String categoryId, String sellerId,
            BigDecimal minPrice, BigDecimal maxPrice,
            String sort, int page, int limit) {
        Pageable pageable = buildPageable(sort, page, limit);
        Page<Product> result = (q == null || q.isBlank())
                ? productRepository.browse(categoryId, sellerId, minPrice, maxPrice, pageable)
                : productRepository.search(q, categoryId, sellerId, minPrice, maxPrice, pageable);
        return toPageResponse(result);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(String id, String viewerRole, String viewerId) {
        Product product = findById(id);
        if (ROLE_ADMIN.equals(viewerRole)) {
            return ProductResponse.from(product);
        }
        if (ROLE_SELLER.equals(viewerRole) && product.getSellerId().equals(viewerId)) {
            return ProductResponse.from(product);
        }
        if (product.getStatus() == ProductStatus.APPROVED && !product.isDeleted()) {
            return ProductResponse.from(product);
        }
        throw new NotFoundException("Product not found: " + id);
    }

    public ProductResponse updateProduct(String id, String sellerId, ProductUpdateRequest request) {
        Product product = findOwned(id, sellerId);
        if (product.isDeleted()) {
            throw new GoneException("Product already deleted");
        }

        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getCategoryId() != null) {
            product.setCategory(findCategory(request.getCategoryId()));
        }
        if (request.getBasePrice() != null) {
            product.setBasePrice(request.getBasePrice());
        }
        if (request.getImageUrl() != null) {
            product.setImageUrl(request.getImageUrl());
        }

        if (product.getStatus() == ProductStatus.REJECTED) {
            product.setStatus(ProductStatus.PENDING_APPROVAL);
            product.setRejectionReason(null);
        }

        return ProductResponse.from(productRepository.save(product));
    }

    public void deleteProduct(String id, String sellerId) {
        Product product = findOwned(id, sellerId);
        if (product.isDeleted()) {
            throw new GoneException("Product already deleted");
        }
        product.setDeletedAt(LocalDateTime.now());
        productRepository.save(product);
    }

    public ProductResponse approveProduct(String id) {
        Product product = findById(id);
        if (product.getStatus() != ProductStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only pending products can be approved");
        }
        product.setStatus(ProductStatus.APPROVED);
        product.setRejectionReason(null);
        return ProductResponse.from(productRepository.save(product));
    }

    public ProductResponse rejectProduct(String id, String reason) {
        Product product = findById(id);
        if (product.getStatus() != ProductStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only pending products can be rejected");
        }
        product.setStatus(ProductStatus.REJECTED);
        product.setRejectionReason(reason);
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> listSellerProducts(
            String sellerId, ProductStatus status, boolean includeDeleted,
            String sort, int page, int limit) {
        Pageable pageable = buildPageable(sort, page, limit);
        return toPageResponse(productRepository.findBySeller(sellerId, status, includeDeleted, pageable));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> listAdminProducts(
            ProductStatus status, boolean includeDeleted, String sort, int page, int limit) {
        Pageable pageable = buildPageable(sort, page, limit);
        return toPageResponse(productRepository.findAllForAdmin(status, includeDeleted, pageable));
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
        List<Product> found = productRepository.findApprovedByIds(uniqueIds);
        Map<String, ProductLookupItem> foundMap = found.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        product -> new ProductLookupItem(product.getId(), product.getBasePrice(), product.getImageUrl())));

        ProductLookupResponse response = new ProductLookupResponse();
        response.setFound(new ArrayList<>(foundMap.values()));
        response.setNotFound(uniqueIds.stream()
                .filter(id -> !foundMap.containsKey(id))
                .collect(Collectors.toList()));
        return response;
    }

    private Product findOwned(String id, String sellerId) {
        Product product = findById(id);
        if (!product.getSellerId().equals(sellerId)) {
            throw new ForbiddenException("You can only modify your own products");
        }
        return product;
    }

    private Product findById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));
    }

    private Category findCategory(String categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found: " + categoryId));
    }

    private Pageable buildPageable(String sort, int page, int limit) {
        if (page < 1) {
            throw new BadRequestException("page must be >= 1");
        }
        if (limit < 1 || limit > 100) {
            throw new BadRequestException("limit must be between 1 and 100");
        }

        String[] parts = sort == null ? new String[] {"createdAt", "desc"} : sort.split(":");
        String field = parts[0];
        String direction = parts.length > 1 ? parts[1] : "desc";

        if (!SORTABLE_FIELDS.contains(field)) {
            throw new BadRequestException("Invalid sort field: " + field);
        }

        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page - 1, limit, Sort.by(dir, field));
    }

    private PageResponse<ProductResponse> toPageResponse(Page<Product> page) {
        List<ProductResponse> items = page.getContent().stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
        return new PageResponse<>(items, page.getNumber() + 1, page.getSize(), page.getTotalElements());
    }
}
