package com.groupdeal.catalog.controller;

import com.groupdeal.catalog.dto.PageResponse;
import com.groupdeal.catalog.dto.ProductLookupRequest;
import com.groupdeal.catalog.dto.ProductLookupResponse;
import com.groupdeal.catalog.dto.ProductRequest;
import com.groupdeal.catalog.dto.ProductResponse;
import com.groupdeal.catalog.dto.ProductUpdateRequest;
import com.groupdeal.catalog.dto.RejectRequest;
import com.groupdeal.catalog.entity.ProductStatus;
import com.groupdeal.catalog.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @RequestHeader("X-User-Id") String sellerId,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(sellerId, request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> browseOrSearch(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String sellerId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(productService.searchProducts(
                q, categoryId, sellerId, minPrice, maxPrice, sort, page, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(productService.getProduct(id, role, userId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String sellerId,
            @Valid @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, sellerId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String sellerId) {
        productService.deleteProduct(id, sellerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/lookup")
    public ResponseEntity<ProductLookupResponse> lookupProducts(
            @Valid @RequestBody ProductLookupRequest request) {
        return ResponseEntity.ok(productService.lookupProducts(request.getProductIds()));
    }

    @GetMapping("/sellers/{sellerId}")
    public ResponseEntity<PageResponse<ProductResponse>> getSellerProducts(
            @PathVariable String sellerId,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(productService.listSellerProducts(
                sellerId, status, includeDeleted, sort, page, limit));
    }

    @GetMapping("/admin")
    public ResponseEntity<PageResponse<ProductResponse>> getAdminProducts(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(productService.listAdminProducts(
                status, includeDeleted, sort, page, limit));
    }

    @PatchMapping("/admin/{id}/approve")
    public ResponseEntity<ProductResponse> approveProduct(@PathVariable String id) {
        return ResponseEntity.ok(productService.approveProduct(id));
    }

    @PatchMapping("/admin/{id}/reject")
    public ResponseEntity<ProductResponse> rejectProduct(
            @PathVariable String id,
            @RequestBody(required = false) RejectRequest request) {
        String reason = request == null ? null : request.getReason();
        return ResponseEntity.ok(productService.rejectProduct(id, reason));
    }
}
