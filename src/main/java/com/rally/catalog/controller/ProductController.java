package com.rally.catalog.controller;

import com.rally.catalog.dto.*;
import com.rally.catalog.entity.ProductStatus;
import com.rally.catalog.entity.Role;
import com.rally.catalog.service.ImageStorageService;
import com.rally.catalog.service.ProductService;
import com.rally.common.exceptions.shared.UnauthorizedException;
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
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final ImageStorageService imageStorageService;

    public ProductController(ProductService productService, ImageStorageService imageStorageService) {
        this.productService = productService;
        this.imageStorageService = imageStorageService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @RequestHeader("X-User-Id") UUID sellerId,
            @RequestHeader("X-User-Name") String sellerName,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(sellerId, sellerName, request));
    }

    @PostMapping("/images")
    public ResponseEntity<ImageUploadResponse> uploadImage(
            @RequestHeader("X-User-Id") UUID sellerId,
            @RequestHeader("X-User-Role") String role,
            @RequestParam("file") MultipartFile file) {
        Role callerRole = Role.fromValue(role);
        if (callerRole != Role.SELLER && callerRole != Role.ADMIN) {
            throw new UnauthorizedException("Only sellers and admins can upload product images");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ImageUploadResponse(imageStorageService.store(file)));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> browseOrSearch(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID sellerId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(productService.searchProducts(
                q, tag, categoryId, sellerId, minPrice, maxPrice, sort, page, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.ok(productService.getProduct(id, Role.fromValue(role), userId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID sellerId,
            @Valid @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, sellerId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID sellerId) {
        productService.deleteProduct(id, sellerId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/restore")
    public ResponseEntity<ProductResponse> restoreProduct(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID sellerId) {
        return ResponseEntity.ok(productService.restoreProduct(id, sellerId));
    }

    @GetMapping("/sellers/{sellerId}")
    public ResponseEntity<PageResponse<ProductResponse>> getSellerProducts(
            @PathVariable UUID sellerId,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "false") boolean deleted,
            @RequestParam(defaultValue = "true") boolean includeDeleted,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(productService.listSellerProducts(
                sellerId, status, includeDeleted, deleted, sort, page, limit));
    }

    @GetMapping("/admin")
    public ResponseEntity<PageResponse<ProductResponse>> getAdminProducts(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "true") boolean includeDeleted,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        // ADMIN enforcement via AdminRoleFilter (com.rally.catalog.config) — disabled
        // until the Auth service is implemented. See SecurityConfig.
        return ResponseEntity.ok(productService.listAdminProducts(
                status, includeDeleted, sort, page, limit));
    }

    @PatchMapping("/admin/{id}/approve")
    public ResponseEntity<ProductResponse> approveProduct(@PathVariable UUID id) {
        // ADMIN enforcement via AdminRoleFilter — disabled until Auth service exists.
        return ResponseEntity.ok(productService.approveProduct(id));
    }

    @PatchMapping("/admin/{id}/reject")
    public ResponseEntity<ProductResponse> rejectProduct(
            @PathVariable UUID id,
            @RequestBody(required = false) RejectRequest request) {
        // ADMIN enforcement via AdminRoleFilter — disabled until Auth service exists.
        String reason = request == null ? null : request.getReason();
        return ResponseEntity.ok(productService.rejectProduct(id, reason));
    }
}
