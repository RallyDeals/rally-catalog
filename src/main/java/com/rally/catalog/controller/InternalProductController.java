package com.rally.catalog.controller;

import com.rally.catalog.dto.DealProductResponse;
import com.rally.catalog.dto.ProductLookupRequest;
import com.rally.catalog.dto.ProductLookupResponse;
import com.rally.catalog.dto.SellerSummaryResponse;
import com.rally.catalog.service.InternalProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import java.util.List;

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
    
    @PostMapping("/batch")
    public ResponseEntity<List<DealProductResponse>> getDealProductResponse(@RequestBody List<UUID> productIds) {
        return ResponseEntity.ok(productService.getDealProductsResponse(productIds));
    }

    @PostMapping("/sellers-summary")
    public ResponseEntity<SellerSummaryResponse> getSellersSummary(@RequestBody List<String> sellerIds) {
        return ResponseEntity.ok(productService.getSellersInfo(sellerIds));
    }
}
