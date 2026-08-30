package com.rally.catalog.controller;

import com.rally.catalog.dto.DealProductResponse;
import com.rally.catalog.dto.ProductLookupRequest;
import com.rally.catalog.dto.ProductLookupResponse;
import com.rally.catalog.service.InternalProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/products")
public class InternalProductController {
    private final InternalProductService productService;

    public InternalProductController(InternalProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/lookup")
    public ResponseEntity<ProductLookupResponse> lookupProducts(
            @Valid @RequestBody ProductLookupRequest request) {
        return ResponseEntity.ok(productService.lookupProducts(request.getProductIds()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DealProductResponse> getDealProductResponse(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getDealProductResponse(id));
    }
}
