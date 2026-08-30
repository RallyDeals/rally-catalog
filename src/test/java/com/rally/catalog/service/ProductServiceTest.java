package com.rally.catalog.service;

import com.rally.catalog.client.DealServiceClient;
import com.rally.catalog.dto.PageResponse;
import com.rally.catalog.dto.ProductLookupResponse;
import com.rally.catalog.dto.ProductRequest;
import com.rally.catalog.dto.ProductResponse;
import com.rally.catalog.dto.ProductUpdateRequest;
import com.rally.catalog.entity.Category;
import com.rally.catalog.entity.Product;
import com.rally.catalog.entity.ProductStatus;
import com.rally.catalog.entity.Role;
import com.rally.catalog.event.ProductCreatedEvent;
import com.rally.catalog.event.ProductDeletedEvent;
import com.rally.catalog.exception.GoneException;
import com.rally.catalog.mapper.CatalogMapperImpl;
import com.rally.catalog.repository.CategoryRepository;
import com.rally.catalog.repository.ProductRepository;
import com.rally.common.exceptions.domain.catalog.ProductNotOwnedException;
import com.rally.common.exceptions.shared.BadRequestException;
import com.rally.common.exceptions.shared.ConflictException;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private DealServiceClient dealServiceClient;

    @Mock
    private KafkaTemplate<String, ProductCreatedEvent> productCreatedKafkaTemplate;

    @Mock
    private KafkaTemplate<String, ProductDeletedEvent> productDeletedKafkaTemplate;

    private ProductService productService;
    private InternalProductService internalProductService;

    private static final UUID SELLER = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID BUYER = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID ADMIN = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID OTHER = UUID.fromString("44444444-4444-4444-8444-444444444444");
    private static final UUID CAT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID PROD_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @BeforeEach
    void setUp() {
        productService = new ProductService(
                productRepository, categoryRepository, new CatalogMapperImpl(), dealServiceClient,
                productCreatedKafkaTemplate, productDeletedKafkaTemplate);
        internalProductService = new InternalProductService(new CatalogMapperImpl(), productRepository);
    }

    private Category category() {
        Category category = new Category("Electronics", "Gadgets");
        category.setId(CAT_ID);
        return category;
    }

    private Product product(ProductStatus status) {
        Product product = new Product(
                SELLER, "Jane Seller", "Headphones", "Noise cancelling", category(),
                new BigDecimal("79.99"), "https://cdn.example.com/img.jpg");
        product.setId(PROD_ID);
        product.setStatus(status);
        product.setCreatedAt(LocalDateTime.now());
        return product;
    }

    @Test
    void createProduct_shouldCreatePendingProduct() {
        when(categoryRepository.findById(CAT_ID)).thenReturn(Optional.of(category()));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(PROD_ID);
            return p;
        });

        ProductRequest request = new ProductRequest();
        request.setName("Headphones");
        request.setCategoryId(CAT_ID);
        request.setBasePrice(new BigDecimal("79.99"));

        ProductResponse response = productService.createProduct(SELLER, "Jane Seller", request);

        assertEquals(ProductStatus.PENDING_APPROVAL, response.getStatus());
        assertEquals(SELLER, response.getSellerId());
        assertEquals("Jane Seller", response.getSellerName());
        assertEquals("Electronics", response.getCategory().getName());
    }

    @Test
    void createProduct_shouldPersistSkuVisibilityAndTags() {
        when(categoryRepository.findById(CAT_ID)).thenReturn(Optional.of(category()));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(PROD_ID);
            return p;
        });

        ProductRequest request = new ProductRequest();
        request.setName("Headphones");
        request.setCategoryId(CAT_ID);
        request.setBasePrice(new BigDecimal("79.99"));
        request.setSku("HP-100-X");
        request.setVisible(false);
        request.setTags(List.of("audio", "wireless"));

        ProductResponse response = productService.createProduct(SELLER, "Jane Seller", request);

        assertEquals("HP-100-X", response.getSku());
        assertFalse(response.isVisible());
        assertEquals(List.of("audio", "wireless"), response.getTags());
    }

    @Test
    void createProduct_shouldDefaultToVisible() {
        when(categoryRepository.findById(CAT_ID)).thenReturn(Optional.of(category()));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductRequest request = new ProductRequest();
        request.setName("Headphones");
        request.setCategoryId(CAT_ID);
        request.setBasePrice(new BigDecimal("79.99"));

        ProductResponse response = productService.createProduct(SELLER, "Jane Seller", request);

        assertTrue(response.isVisible());
    }

    @Test
    void updateProduct_shouldApplySkuVisibilityAndTags() {
        Product approved = product(ProductStatus.APPROVED);
        when(productRepository.findById(PROD_ID)).thenReturn(Optional.of(approved));
        when(productRepository.save(any(Product.class))).thenReturn(approved);

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setSku("HP-PRO-X");
        request.setVisible(false);
        request.setTags(List.of("premium", "noise-cancelling"));

        ProductResponse response = productService.updateProduct(PROD_ID, SELLER, request);

        assertEquals("HP-PRO-X", response.getSku());
        assertFalse(response.isVisible());
        assertEquals(List.of("premium", "noise-cancelling"), response.getTags());
    }

    @Test
    void updateProduct_shouldNotOverwriteVisibilityWhenNull() {
        Product approved = product(ProductStatus.APPROVED);
        approved.setVisible(false);
        when(productRepository.findById(PROD_ID)).thenReturn(Optional.of(approved));
        when(productRepository.save(any(Product.class))).thenReturn(approved);

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setName("Headphones Pro");

        ProductResponse response = productService.updateProduct(PROD_ID, SELLER, request);

        assertFalse(response.isVisible());
    }

    @Test
    void createProduct_shouldThrowWhenCategoryMissing() {
        UUID missingId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        when(categoryRepository.findById(missingId)).thenReturn(Optional.empty());

        ProductRequest request = new ProductRequest();
        request.setName("Headphones");
        request.setCategoryId(missingId);
        request.setBasePrice(new BigDecimal("79.99"));

        assertThrows(NotFoundException.class, () -> productService.createProduct(SELLER, "Jane Seller", request));
    }

    @Test
    void getProduct_shouldHideUnapprovedFromBuyer() {
        Product pending = product(ProductStatus.PENDING_APPROVAL);
        when(productRepository.findById(PROD_ID)).thenReturn(Optional.of(pending));

        assertThrows(NotFoundException.class, () -> productService.getProduct(PROD_ID, Role.BUYER, BUYER));
    }

    @Test
    void getProduct_shouldExposeApprovedToBuyer() {
        Product approved = product(ProductStatus.APPROVED);
        when(productRepository.findById(PROD_ID)).thenReturn(Optional.of(approved));

        ProductResponse response = productService.getProduct(PROD_ID, Role.BUYER, BUYER);

        assertEquals(PROD_ID, response.getId());
    }

    @Test
    void getProduct_shouldHideHiddenApprovedProductFromBuyer() {
        Product approved = product(ProductStatus.APPROVED);
        approved.setVisible(false);
        when(productRepository.findById(PROD_ID)).thenReturn(Optional.of(approved));

        assertThrows(NotFoundException.class, () -> productService.getProduct(PROD_ID, Role.BUYER, BUYER));
    }

    @Test
    void getProduct_shouldExposeHiddenProductToOwnerSeller() {
        Product approved = product(ProductStatus.APPROVED);
        approved.setVisible(false);
        when(productRepository.findById(PROD_ID)).thenReturn(Optional.of(approved));

        ProductResponse response = productService.getProduct(PROD_ID, Role.SELLER, SELLER);

        assertEquals(PROD_ID, response.getId());
    }

    @Test
    void getProduct_shouldExposeOwnProductToSeller() {
        Product rejected = product(ProductStatus.REJECTED);
        when(productRepository.findById(PROD_ID)).thenReturn(Optional.of(rejected));

        ProductResponse response = productService.getProduct(PROD_ID, Role.SELLER, SELLER);

        assertEquals(ProductStatus.REJECTED, response.getStatus());
    }

    @Test
    void getProduct_shouldHideOtherSellersUnapprovedProduct() {
        Product pending = product(ProductStatus.PENDING_APPROVAL);
        when(productRepository.findById(PROD_ID)).thenReturn(Optional.of(pending));

        assertThrows(NotFoundException.class, () -> productService.getProduct(PROD_ID, Role.SELLER, OTHER));
    }

    @Test
    void getProduct_shouldExposeApprovedProductToOtherSeller() {
        Product approved = product(ProductStatus.APPROVED);
        when(productRepository.findById(PROD_ID)).thenReturn(Optional.of(approved));

        ProductResponse response = productService.getProduct(PROD_ID, Role.SELLER, OTHER);

        assertEquals(PROD_ID, response.getId());
    }

    @Test
    void getProduct_shouldExposeAnyToAdmin() {
        Product pending = product(ProductStatus.PENDING_APPROVAL);
        when(productRepository.findById(PROD_ID)).thenReturn(Optional.of(pending));

        ProductResponse response = productService.getProduct(PROD_ID, Role.ADMIN, ADMIN);

        assertEquals(ProductStatus.PENDING_APPROVAL, response.getStatus());
    }

    @Test
    void updateProduct_shouldApplyPartialUpdate() {
        Product approved = product(ProductStatus.APPROVED);
        when(productRepository.findById(PROD_ID)).thenReturn(Optional.of(approved));
        when(productRepository.save(any(Product.class))).thenReturn(approved);

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setName("Headphones Pro");

        ProductResponse response = productService.updateProduct(PROD_ID, SELLER, request);

        assertEquals("Headphones Pro", response.getName());
        assertEquals(new BigDecimal("79.99"), response.getBasePrice());
        assertEquals(ProductStatus.APPROVED, response.getStatus());
    }

    @Test
    void updateProduct_shouldResetRejectedToPendingApproval() {
        Product rejected = product(ProductStatus.REJECTED);
        rejected.setRejectionReason("Missing info");
        when(productRepository.findById(PROD_ID)).thenReturn(Optional.of(rejected));
        when(productRepository.save(any(Product.class))).thenReturn(rejected);

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setName("Headphones Pro");

        ProductResponse response = productService.updateProduct(PROD_ID, SELLER, request);

        assertEquals(ProductStatus.PENDING_APPROVAL, response.getStatus());
        assertEquals(null, response.getRejectionReason());
    }

    @Test
    void updateProduct_shouldRejectNonOwner() {
        Product approved = product(ProductStatus.APPROVED);
        when(productRepository.findById(PROD_ID)).thenReturn(Optional.of(approved));

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setName("Hijacked");

        assertThrows(ProductNotOwnedException.class, () -> productService.updateProduct(PROD_ID, OTHER, request));
    }

    @Test
    void updateProduct_shouldRejectDeletedProduct() {
        Product approved = product(ProductStatus.APPROVED);
        approved.setDeletedAt(LocalDateTime.now());
        when(productRepository.findById(PROD_ID)).thenReturn(Optional.of(approved));

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setName("Headphones Pro");

        assertThrows(GoneException.class, () -> productService.updateProduct(PROD_ID, SELLER, request));
    }

    @Test
    void deleteProduct_shouldSoftDelete() {
        Product approved = product(ProductStatus.APPROVED);
        when(productRepository.findById(PROD_ID)).thenReturn(Optional.of(approved));
        when(productRepository.save(any(Product.class))).thenReturn(approved);
        when(dealServiceClient.hasActiveDeal(PROD_ID.toString())).thenReturn(false);

        productService.deleteProduct(PROD_ID, SELLER);

        assertNotNull(approved.getDeletedAt());
        verify(productRepository).save(approved);
    }

    @Test
    void deleteProduct_shouldRejectWhenTiedToActiveDeal() {
        Product approved = product(ProductStatus.APPROVED);
        when(productRepository.findById(PROD_ID)).thenReturn(Optional.of(approved));
        when(dealServiceClient.hasActiveDeal(PROD_ID.toString())).thenReturn(true);

        assertThrows(ConflictException.class, () -> productService.deleteProduct(PROD_ID, SELLER));

        assertNull(approved.getDeletedAt());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteProduct_shouldRejectAlreadyDeleted() {
        Product approved = product(ProductStatus.APPROVED);
        approved.setDeletedAt(LocalDateTime.now());
        when(productRepository.findById(PROD_ID)).thenReturn(Optional.of(approved));

        assertThrows(GoneException.class, () -> productService.deleteProduct(PROD_ID, SELLER));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteProduct_shouldRejectNonOwner() {
        Product approved = product(ProductStatus.APPROVED);
        when(productRepository.findById(PROD_ID)).thenReturn(Optional.of(approved));

        assertThrows(ProductNotOwnedException.class, () -> productService.deleteProduct(PROD_ID, OTHER));
    }

    @Test
    void restoreProduct_shouldClearDeletedAtAndResubmitForApproval() {
        Product approved = product(ProductStatus.APPROVED);
        approved.setDeletedAt(LocalDateTime.now());
        approved.setRejectionReason("removed by seller");
        when(productRepository.findById(PROD_ID)).thenReturn(Optional.of(approved));
        when(productRepository.save(any(Product.class))).thenReturn(approved);

        productService.restoreProduct(PROD_ID, SELLER);

        assertNull(approved.getDeletedAt());
        assertEquals(ProductStatus.PENDING_APPROVAL, approved.getStatus());
        assertNull(approved.getRejectionReason());
        verify(productRepository).save(approved);
    }

    @Test
    void restoreProduct_shouldRejectWhenNotDeleted() {
        Product approved = product(ProductStatus.APPROVED);
        when(productRepository.findById(PROD_ID)).thenReturn(Optional.of(approved));

        assertThrows(ConflictException.class, () -> productService.restoreProduct(PROD_ID, SELLER));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void restoreProduct_shouldRejectNonOwner() {
        Product approved = product(ProductStatus.APPROVED);
        approved.setDeletedAt(LocalDateTime.now());
        when(productRepository.findById(PROD_ID)).thenReturn(Optional.of(approved));

        assertThrows(ProductNotOwnedException.class, () -> productService.restoreProduct(PROD_ID, OTHER));
    }

    @Test
    void approveProduct_shouldApprovePendingProduct() {
        Product pending = product(ProductStatus.PENDING_APPROVAL);
        when(productRepository.findById(PROD_ID)).thenReturn(Optional.of(pending));
        when(productRepository.save(any(Product.class))).thenReturn(pending);

        ProductResponse response = productService.approveProduct(PROD_ID);

        assertEquals(ProductStatus.APPROVED, response.getStatus());
    }

    @Test
    void approveProduct_shouldRejectAlreadyApproved() {
        Product approved = product(ProductStatus.APPROVED);
        when(productRepository.findById(PROD_ID)).thenReturn(Optional.of(approved));

        assertThrows(BadRequestException.class, () -> productService.approveProduct(PROD_ID));
    }

    @Test
    void rejectProduct_shouldSetReason() {
        Product pending = product(ProductStatus.PENDING_APPROVAL);
        when(productRepository.findById(PROD_ID)).thenReturn(Optional.of(pending));
        when(productRepository.save(any(Product.class))).thenReturn(pending);

        ProductResponse response = productService.rejectProduct(PROD_ID, "Missing info");

        assertEquals(ProductStatus.REJECTED, response.getStatus());
        assertEquals("Missing info", response.getRejectionReason());
    }

    @Test
    void rejectProduct_shouldRejectAlreadyApproved() {
        Product approved = product(ProductStatus.APPROVED);
        when(productRepository.findById(PROD_ID)).thenReturn(Optional.of(approved));

        assertThrows(BadRequestException.class, () -> productService.rejectProduct(PROD_ID, "nope"));
    }

    @Test
    void searchProducts_shouldReturnApprovedProducts() {
        Page<Product> page = new PageImpl<>(List.of(product(ProductStatus.APPROVED)), PageRequest.of(0, 20), 1);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<ProductResponse> response = productService.searchProducts(null, null, null, null, null, null, null, 1, 20);

        verify(productRepository).findAll(any(Specification.class), any(Pageable.class));
        assertEquals(1, response.getItems().size());
        assertEquals(20, response.getLimit());
    }

    @Test
    void searchProducts_shouldApplyKeywordToCriteriaQuery() {
        Page<Product> page = new PageImpl<>(List.of(product(ProductStatus.APPROVED)), PageRequest.of(0, 20), 1);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<ProductResponse> response = productService.searchProducts("headphones", null, null, null, null, null, null, 1, 20);

        verify(productRepository).findAll(any(Specification.class), any(Pageable.class));
        assertEquals(1, response.getItems().size());
    }

    @Test
    void searchProducts_shouldApplyTagFilterToCriteriaQuery() {
        Page<Product> page = new PageImpl<>(List.of(product(ProductStatus.APPROVED)), PageRequest.of(0, 20), 1);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<ProductResponse> response = productService.searchProducts(null, "wireless", null, null, null, null, null, 1, 20);

        verify(productRepository).findAll(any(Specification.class), any(Pageable.class));
        assertEquals(1, response.getItems().size());
    }

    @Test
    void searchProducts_shouldRejectInvalidSortField() {
        assertThrows(BadRequestException.class,
                () -> productService.searchProducts(null, null, null, null, null, null, "price", 1, 20));
    }

    @Test
    void searchProducts_shouldRejectOutOfRangePagination() {
        assertThrows(BadRequestException.class,
                () -> productService.searchProducts(null, null, null, null, null, null, null, 0, 20));
        assertThrows(BadRequestException.class,
                () -> productService.searchProducts(null, null, null, null, null, null, null, 1, 101));
    }

    @Test
    void listSellerProducts_shouldFilterBySeller() {
        Page<Product> page = new PageImpl<>(List.of(product(ProductStatus.APPROVED)));
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<ProductResponse> response = productService.listSellerProducts(SELLER, null, false, false, null, 1, 20);

        assertEquals(1, response.getItems().size());
    }

    @Test
    void listAdminProducts_shouldReturnAllWithStatus() {
        Page<Product> page = new PageImpl<>(List.of(product(ProductStatus.PENDING_APPROVAL)));
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<ProductResponse> response = productService.listAdminProducts(ProductStatus.PENDING_APPROVAL, false, null, 1, 20);

        assertEquals(ProductStatus.PENDING_APPROVAL, response.getItems().get(0).getStatus());
    }

    @Test
    void lookupProducts_shouldSplitFoundAndNotFound() {
        Product approved = product(ProductStatus.APPROVED);
        when(productRepository.findAll(any(Specification.class))).thenReturn(List.of(approved));

        ProductLookupResponse response = internalProductService.lookupProducts(List.of(PROD_ID, UUID.randomUUID()));

        assertEquals(1, response.getFound().size());
        assertTrue(response.getFound().containsKey(PROD_ID));
        assertEquals(PROD_ID, response.getFound().get(PROD_ID).getId());
        assertEquals(1, response.getNotFound().size());
    }

    @Test
    void lookupProducts_shouldReturnFirstGalleryImageAsCover() {
        Product approved = product(ProductStatus.APPROVED);
        approved.setImages(List.of("https://cdn.example.com/one.jpg", "https://cdn.example.com/two.jpg"));
        when(productRepository.findAll(any(Specification.class))).thenReturn(List.of(approved));

        ProductLookupResponse response = internalProductService.lookupProducts(List.of(PROD_ID));

        assertEquals("https://cdn.example.com/one.jpg", response.getFound().get(PROD_ID).getImageUrl());
    }

    @Test
    void lookupProducts_shouldFallBackToImageUrlColumnWhenGalleryEmpty() {
        Product approved = product(ProductStatus.APPROVED);
        when(productRepository.findAll(any(Specification.class))).thenReturn(List.of(approved));

        ProductLookupResponse response = internalProductService.lookupProducts(List.of(PROD_ID));

        assertEquals("https://cdn.example.com/img.jpg", response.getFound().get(PROD_ID).getImageUrl());
    }

    @Test
    void lookupProducts_shouldRejectEmpty() {
        assertThrows(BadRequestException.class, () -> internalProductService.lookupProducts(List.of()));
    }

    @Test
    void lookupProducts_shouldRejectTooManyIds() {
        List<UUID> tooMany = java.util.stream.IntStream.rangeClosed(1, 51)
                .mapToObj(i -> UUID.randomUUID()).toList();

        assertThrows(BadRequestException.class, () -> internalProductService.lookupProducts(tooMany));
    }
}
