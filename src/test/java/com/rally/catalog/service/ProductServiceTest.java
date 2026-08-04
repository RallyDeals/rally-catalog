package com.rally.catalog.service;

import com.rally.catalog.dto.PageResponse;
import com.rally.catalog.dto.ProductLookupResponse;
import com.rally.catalog.dto.ProductRequest;
import com.rally.catalog.dto.ProductResponse;
import com.rally.catalog.dto.ProductUpdateRequest;
import com.rally.catalog.entity.Category;
import com.rally.catalog.entity.Product;
import com.rally.catalog.entity.ProductStatus;
import com.rally.catalog.exception.GoneException;
import com.rally.catalog.repository.CategoryRepository;
import com.rally.catalog.repository.ProductRepository;
import com.rally.common.exceptions.domain.catalog.ProductNotOwnedException;
import com.rally.common.exceptions.shared.BadRequestException;
import com.rally.common.exceptions.shared.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, categoryRepository);
    }

    private Category category() {
        Category category = new Category("Electronics", "Gadgets");
        category.setId("cat-1");
        return category;
    }

    private Product product(ProductStatus status) {
        Product product = new Product(
                "seller-1", "Headphones", "Noise cancelling", category(),
                new BigDecimal("79.99"), "https://cdn.example.com/img.jpg");
        product.setId("prod-1");
        product.setStatus(status);
        product.setCreatedAt(LocalDateTime.now());
        return product;
    }

    @Test
    void createProduct_shouldCreatePendingProduct() {
        when(categoryRepository.findById("cat-1")).thenReturn(Optional.of(category()));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId("prod-1");
            return p;
        });

        ProductRequest request = new ProductRequest();
        request.setName("Headphones");
        request.setCategoryId("cat-1");
        request.setBasePrice(new BigDecimal("79.99"));

        ProductResponse response = productService.createProduct("seller-1", request);

        assertEquals(ProductStatus.PENDING_APPROVAL, response.getStatus());
        assertEquals("seller-1", response.getSellerId());
        assertEquals("Electronics", response.getCategory().getName());
    }

    @Test
    void createProduct_shouldThrowWhenCategoryMissing() {
        when(categoryRepository.findById("missing")).thenReturn(Optional.empty());

        ProductRequest request = new ProductRequest();
        request.setName("Headphones");
        request.setCategoryId("missing");
        request.setBasePrice(new BigDecimal("79.99"));

        assertThrows(NotFoundException.class, () -> productService.createProduct("seller-1", request));
    }

    @Test
    void getProduct_shouldHideUnapprovedFromBuyer() {
        Product pending = product(ProductStatus.PENDING_APPROVAL);
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(pending));

        assertThrows(NotFoundException.class, () -> productService.getProduct("prod-1", "BUYER", "buyer-1"));
    }

    @Test
    void getProduct_shouldExposeApprovedToBuyer() {
        Product approved = product(ProductStatus.APPROVED);
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(approved));

        ProductResponse response = productService.getProduct("prod-1", "BUYER", "buyer-1");

        assertEquals("prod-1", response.getId());
    }

    @Test
    void getProduct_shouldExposeOwnProductToSeller() {
        Product rejected = product(ProductStatus.REJECTED);
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(rejected));

        ProductResponse response = productService.getProduct("prod-1", "SELLER", "seller-1");

        assertEquals(ProductStatus.REJECTED, response.getStatus());
    }

    @Test
    void getProduct_shouldHideOtherSellersUnapprovedProduct() {
        Product pending = product(ProductStatus.PENDING_APPROVAL);
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(pending));

        assertThrows(NotFoundException.class, () -> productService.getProduct("prod-1", "SELLER", "other-seller"));
    }

    @Test
    void getProduct_shouldExposeApprovedProductToOtherSeller() {
        Product approved = product(ProductStatus.APPROVED);
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(approved));

        ProductResponse response = productService.getProduct("prod-1", "SELLER", "other-seller");

        assertEquals("prod-1", response.getId());
    }

    @Test
    void getProduct_shouldExposeAnyToAdmin() {
        Product pending = product(ProductStatus.PENDING_APPROVAL);
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(pending));

        ProductResponse response = productService.getProduct("prod-1", "ADMIN", "admin-1");

        assertEquals(ProductStatus.PENDING_APPROVAL, response.getStatus());
    }

    @Test
    void updateProduct_shouldApplyPartialUpdate() {
        Product approved = product(ProductStatus.APPROVED);
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(approved));
        when(productRepository.save(any(Product.class))).thenReturn(approved);

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setName("Headphones Pro");

        ProductResponse response = productService.updateProduct("prod-1", "seller-1", request);

        assertEquals("Headphones Pro", response.getName());
        assertEquals(new BigDecimal("79.99"), response.getBasePrice());
        assertEquals(ProductStatus.APPROVED, response.getStatus());
    }

    @Test
    void updateProduct_shouldResetRejectedToPendingApproval() {
        Product rejected = product(ProductStatus.REJECTED);
        rejected.setRejectionReason("Missing info");
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(rejected));
        when(productRepository.save(any(Product.class))).thenReturn(rejected);

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setName("Headphones Pro");

        ProductResponse response = productService.updateProduct("prod-1", "seller-1", request);

        assertEquals(ProductStatus.PENDING_APPROVAL, response.getStatus());
        assertEquals(null, response.getRejectionReason());
    }

    @Test
    void updateProduct_shouldRejectNonOwner() {
        Product approved = product(ProductStatus.APPROVED);
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(approved));

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setName("Hijacked");

        assertThrows(ProductNotOwnedException.class, () -> productService.updateProduct("prod-1", "other-seller", request));
    }

    @Test
    void updateProduct_shouldRejectDeletedProduct() {
        Product approved = product(ProductStatus.APPROVED);
        approved.setDeletedAt(LocalDateTime.now());
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(approved));

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setName("Headphones Pro");

        assertThrows(GoneException.class, () -> productService.updateProduct("prod-1", "seller-1", request));
    }

    @Test
    void deleteProduct_shouldSoftDelete() {
        Product approved = product(ProductStatus.APPROVED);
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(approved));
        when(productRepository.save(any(Product.class))).thenReturn(approved);

        productService.deleteProduct("prod-1", "seller-1");

        assertNotNull(approved.getDeletedAt());
        verify(productRepository).save(approved);
    }

    @Test
    void deleteProduct_shouldRejectAlreadyDeleted() {
        Product approved = product(ProductStatus.APPROVED);
        approved.setDeletedAt(LocalDateTime.now());
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(approved));

        assertThrows(GoneException.class, () -> productService.deleteProduct("prod-1", "seller-1"));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteProduct_shouldRejectNonOwner() {
        Product approved = product(ProductStatus.APPROVED);
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(approved));

        assertThrows(ProductNotOwnedException.class, () -> productService.deleteProduct("prod-1", "other-seller"));
    }

    @Test
    void approveProduct_shouldApprovePendingProduct() {
        Product pending = product(ProductStatus.PENDING_APPROVAL);
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(pending));
        when(productRepository.save(any(Product.class))).thenReturn(pending);

        ProductResponse response = productService.approveProduct("prod-1");

        assertEquals(ProductStatus.APPROVED, response.getStatus());
    }

    @Test
    void approveProduct_shouldRejectAlreadyApproved() {
        Product approved = product(ProductStatus.APPROVED);
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(approved));

        assertThrows(BadRequestException.class, () -> productService.approveProduct("prod-1"));
    }

    @Test
    void rejectProduct_shouldSetReason() {
        Product pending = product(ProductStatus.PENDING_APPROVAL);
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(pending));
        when(productRepository.save(any(Product.class))).thenReturn(pending);

        ProductResponse response = productService.rejectProduct("prod-1", "Missing info");

        assertEquals(ProductStatus.REJECTED, response.getStatus());
        assertEquals("Missing info", response.getRejectionReason());
    }

    @Test
    void rejectProduct_shouldRejectAlreadyApproved() {
        Product approved = product(ProductStatus.APPROVED);
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(approved));

        assertThrows(BadRequestException.class, () -> productService.rejectProduct("prod-1", "nope"));
    }

    @Test
    void searchProducts_shouldDelegateToBrowseWithoutQuery() {
        Page<Product> page = new PageImpl<>(List.of(product(ProductStatus.APPROVED)), PageRequest.of(0, 20), 1);
        when(productRepository.browse(isNull(), isNull(), isNull(), isNull(), any(Pageable.class))).thenReturn(page);

        PageResponse<ProductResponse> response = productService.searchProducts(null, null, null, null, null, null, 1, 20);

        verify(productRepository).browse(any(), any(), any(), any(), any(Pageable.class));
        verify(productRepository, never()).search(anyString(), any(), any(), any(), any(), any());
        assertEquals(1, response.getItems().size());
        assertEquals(20, response.getLimit());
    }

    @Test
    void searchProducts_shouldDelegateToFullTextSearchWithQuery() {
        Page<Product> page = new PageImpl<>(List.of(product(ProductStatus.APPROVED)));
        when(productRepository.search(eq("headphones"), any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        productService.searchProducts("headphones", null, null, null, null, null, 1, 20);

        verify(productRepository).search(eq("headphones"), any(), any(), any(), any(), any(Pageable.class));
        verify(productRepository, never()).browse(any(), any(), any(), any(), any());
    }

    @Test
    void searchProducts_shouldRejectInvalidSortField() {
        assertThrows(BadRequestException.class,
                () -> productService.searchProducts(null, null, null, null, null, "price", 1, 20));
    }

    @Test
    void searchProducts_shouldRejectOutOfRangePagination() {
        assertThrows(BadRequestException.class,
                () -> productService.searchProducts(null, null, null, null, null, null, 0, 20));
        assertThrows(BadRequestException.class,
                () -> productService.searchProducts(null, null, null, null, null, null, 1, 101));
    }

    @Test
    void listSellerProducts_shouldFilterBySeller() {
        Page<Product> page = new PageImpl<>(List.of(product(ProductStatus.APPROVED)));
        when(productRepository.findBySeller(eq("seller-1"), isNull(), eq(false), any(Pageable.class))).thenReturn(page);

        PageResponse<ProductResponse> response = productService.listSellerProducts("seller-1", null, false, null, 1, 20);

        assertEquals(1, response.getItems().size());
    }

    @Test
    void listAdminProducts_shouldReturnAllWithStatus() {
        Page<Product> page = new PageImpl<>(List.of(product(ProductStatus.PENDING_APPROVAL)));
        when(productRepository.findAllForAdmin(eq(ProductStatus.PENDING_APPROVAL), eq(false), any(Pageable.class))).thenReturn(page);

        PageResponse<ProductResponse> response = productService.listAdminProducts(ProductStatus.PENDING_APPROVAL, false, null, 1, 20);

        assertEquals(ProductStatus.PENDING_APPROVAL, response.getItems().get(0).getStatus());
    }

    @Test
    void lookupProducts_shouldSplitFoundAndNotFound() {
        Product approved = product(ProductStatus.APPROVED);
        when(productRepository.findApprovedByIds(any())).thenReturn(List.of(approved));

        ProductLookupResponse response = productService.lookupProducts(List.of("prod-1", "prod-2"));

        assertEquals(1, response.getFound().size());
        assertEquals("prod-1", response.getFound().get(0).getId());
        assertEquals(List.of("prod-2"), response.getNotFound());
    }

    @Test
    void lookupProducts_shouldRejectEmpty() {
        assertThrows(BadRequestException.class, () -> productService.lookupProducts(List.of()));
    }

    @Test
    void lookupProducts_shouldRejectTooManyIds() {
        List<String> tooMany = java.util.stream.IntStream.rangeClosed(1, 51)
                .mapToObj(String::valueOf).toList();

        assertThrows(BadRequestException.class, () -> productService.lookupProducts(tooMany));
    }
}
