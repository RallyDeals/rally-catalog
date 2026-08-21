package com.rally.catalog.service;

import com.rally.catalog.dto.*;
import com.rally.catalog.client.DealServiceClient;
import com.rally.catalog.entity.Category;
import com.rally.catalog.entity.Product;
import com.rally.catalog.entity.ProductStatus;
import com.rally.catalog.entity.Role;
import com.rally.catalog.exception.GoneException;
import com.rally.catalog.mapper.CatalogMapper;
import com.rally.common.exceptions.shared.ConflictException;
import com.rally.common.exceptions.domain.catalog.ProductNotFoundException;
import com.rally.common.exceptions.domain.catalog.ProductNotOwnedException;
import com.rally.common.exceptions.shared.BadRequestException;
import com.rally.common.exceptions.shared.NotFoundException;
import com.rally.catalog.repository.CategoryRepository;
import com.rally.catalog.repository.ProductRepository;
import com.rally.catalog.repository.ProductSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("createdAt", "basePrice", "name");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CatalogMapper catalogMapper;
    private final DealServiceClient dealServiceClient;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                          CatalogMapper catalogMapper, DealServiceClient dealServiceClient) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.catalogMapper = catalogMapper;
        this.dealServiceClient = dealServiceClient;
    }

    public ProductResponse createProduct(UUID sellerId, String sellerName, ProductRequest request) {
        Category category = findCategory(request.getCategoryId());
        Product product = new Product(
                sellerId.toString(),
                sellerName,
                request.getName(),
                request.getDescription(),
                category,
                request.getBasePrice(),
                request.getImageUrl()
        );
        if (request.getImages() != null) {
            product.setImages(request.getImages());
        }
        if (request.getSku() != null) {
            product.setSku(request.getSku());
        }
        if (request.getVisible() != null) {
            product.setVisible(request.getVisible());
        }
        if (request.getTags() != null) {
            product.setTags(request.getTags());
        }
        return catalogMapper.toProductResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProducts(
            String q, String tag, String categoryId, UUID sellerId,
            BigDecimal minPrice, BigDecimal maxPrice,
            String sort, int page, int limit) {
        Pageable pageable = buildPageable(sort, page, limit);
        Specification<Product> spec = Specification.allOf(
                ProductSpecifications.approvedAndNotDeleted(),
                ProductSpecifications.visible(true),
                ProductSpecifications.keyword(q),
                ProductSpecifications.tagIs(tag),
                ProductSpecifications.categoryIs(categoryId),
                ProductSpecifications.sellerIs(sellerId == null ? null : sellerId.toString()),
                ProductSpecifications.priceBetween(minPrice, maxPrice));
        return toPageResponse(productRepository.findAll(spec, pageable));
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(String id, Role viewerRole, UUID viewerId) {
        Product product = findById(id);
        if (viewerRole == Role.ADMIN) {
            return catalogMapper.toProductResponse(product);
        }
        if (viewerRole == Role.SELLER && viewerId != null
                && product.getSellerId().equals(viewerId.toString())) {
            return catalogMapper.toProductResponse(product);
        }
        if (product.getStatus() == ProductStatus.APPROVED && !product.isDeleted() && product.isVisible()) {
            return catalogMapper.toProductResponse(product);
        }
        throw new ProductNotFoundException(id);
    }

    public ProductResponse updateProduct(String id, UUID sellerId, ProductUpdateRequest request) {
        Product product = findOwned(id, sellerId.toString());
        if (product.isDeleted()) {
            throw new GoneException("Product already deleted");
        }

        catalogMapper.applyUpdate(product, request);
        if (request.getCategoryId() != null) {
            product.setCategory(findCategory(request.getCategoryId()));
        }

        if (product.getStatus() == ProductStatus.REJECTED) {
            product.setStatus(ProductStatus.PENDING_APPROVAL);
            product.setRejectionReason(null);
        }

        return catalogMapper.toProductResponse(productRepository.save(product));
    }

    public void deleteProduct(String id, UUID sellerId) {
        Product product = findOwned(id, sellerId.toString());
        if (product.isDeleted()) {
            throw new GoneException("Product already deleted");
        }
        // Contract: Deal Service GET /internal/deals?productId={id}&active=true (§10.1 of
        // catalog-service.md). Mocked by DealServiceFakeClientImpl outside the prod profile
        // (deal.service.mock.has-active-deal); real client under prod (deal.service.url).
        if (dealServiceClient.hasActiveDeal(product.getId())) {
            throw new ConflictException("Product is tied to an active deal and cannot be deleted");
        }
        product.setDeletedAt(LocalDateTime.now());
        productRepository.save(product);
    }

    public ProductResponse restoreProduct(String id, UUID sellerId) {
        Product product = findOwned(id, sellerId.toString());
        if (!product.isDeleted()) {
            throw new ConflictException("Product is not deleted");
        }
        product.setDeletedAt(null);
        product.setStatus(ProductStatus.PENDING_APPROVAL);
        product.setRejectionReason(null);
        return catalogMapper.toProductResponse(productRepository.save(product));
    }

    public ProductResponse approveProduct(String id) {
        Product product = findById(id);
        if (product.getStatus() != ProductStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only pending products can be approved");
        }
        product.setStatus(ProductStatus.APPROVED);
        product.setRejectionReason(null);
        return catalogMapper.toProductResponse(productRepository.save(product));
    }

    public ProductResponse rejectProduct(String id, String reason) {
        Product product = findById(id);
        if (product.getStatus() != ProductStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only pending products can be rejected");
        }
        product.setStatus(ProductStatus.REJECTED);
        product.setRejectionReason(reason);
        return catalogMapper.toProductResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> listSellerProducts(
            UUID sellerId, ProductStatus status, boolean includeDeleted, boolean deletedOnly,
            String sort, int page, int limit) {
        Pageable pageable = buildPageable(sort, page, limit);
        Specification<Product> spec = Specification.allOf(ProductSpecifications.sellerIs(sellerId.toString()));
        if (deletedOnly) {
            spec = spec.and(ProductSpecifications.deletedOnly());
        } else {
            spec = spec
                    .and(ProductSpecifications.statusIs(status))
                    .and(ProductSpecifications.notDeleted(includeDeleted));
        }
        return toPageResponse(productRepository.findAll(spec, pageable));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> listAdminProducts(
            ProductStatus status, boolean includeDeleted, String sort, int page, int limit) {
        Pageable pageable = buildPageable(sort, page, limit);
        Specification<Product> spec = Specification.allOf(
                ProductSpecifications.statusIs(status),
                ProductSpecifications.notDeleted(includeDeleted));
        return toPageResponse(productRepository.findAll(spec, pageable));
    }

    private Product findOwned(String id, String sellerId) {
        Product product = findById(id);
        if (!product.getSellerId().equals(sellerId)) {
            throw new ProductNotOwnedException(id);
        }
        return product;
    }

    private Product findById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    private Category findCategory(String categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category", categoryId));
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
                .map(catalogMapper::toProductResponse)
                .collect(Collectors.toList());
        return new PageResponse<>(items, page.getNumber() + 1, page.getSize(), page.getTotalElements());
    }
}
