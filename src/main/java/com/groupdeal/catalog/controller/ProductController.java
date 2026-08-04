package com.groupdeal.catalog.controller;

import com.groupdeal.catalog.dto.ProductRequest;
import com.groupdeal.catalog.dto.ProductResponse;
import com.groupdeal.catalog.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // --- Seller endpoints ---

    @PostMapping("/products")
    public ResponseEntity<ProductResponse> createProduct(
            @RequestHeader("X-User-Id") String sellerId,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.createProduct(sellerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    @PatchMapping("/products/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String sellerId,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.updateProduct(id, sellerId, request);
        return ResponseEntity.ok(product);
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String sellerId) {
        productService.deleteProduct(id, sellerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sellers/{sellerId}/products")
    public ResponseEntity<List<ProductResponse>> getSellerProducts(
            @PathVariable String sellerId) {
        return ResponseEntity.ok(productService.getSellerProducts(sellerId));
    }

    // --- Buyer / public endpoints ---

    @GetMapping("/products")
    public ResponseEntity<List<ProductResponse>> browseOrSearch(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sellerId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {

        boolean hasFilters = name != null || category != null || sellerId != null
                || minPrice != null || maxPrice != null;

        if (hasFilters) {
            return ResponseEntity.ok(productService.searchProducts(name, category, sellerId, minPrice, maxPrice));
        }
        return ResponseEntity.ok(productService.browseProducts());
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    // --- Admin endpoints ---

    @GetMapping("/admin/products/pending")
    public ResponseEntity<List<ProductResponse>> getPendingProducts() {
        return ResponseEntity.ok(productService.getPendingProducts());
    }

    @PatchMapping("/admin/products/{id}/approve")
    public ResponseEntity<ProductResponse> approveProduct(@PathVariable String id) {
        return ResponseEntity.ok(productService.approveProduct(id));
    }

    @PatchMapping("/admin/products/{id}/reject")
    public ResponseEntity<ProductResponse> rejectProduct(@PathVariable String id) {
        return ResponseEntity.ok(productService.rejectProduct(id));
    }

    // --- Exception handling ---

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
