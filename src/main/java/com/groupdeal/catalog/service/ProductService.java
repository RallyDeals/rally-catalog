package com.groupdeal.catalog.service;

import com.groupdeal.catalog.dto.ProductRequest;
import com.groupdeal.catalog.dto.ProductResponse;
import com.groupdeal.catalog.entity.Product;
import com.groupdeal.catalog.entity.ProductStatus;
import com.groupdeal.catalog.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse createProduct(String sellerId, ProductRequest request) {
        Product product = new Product(
                sellerId,
                request.getName(),
                request.getDescription(),
                request.getCategory(),
                request.getBasePrice()
        );
        product.setStatus(ProductStatus.PENDING);
        product = productRepository.save(product);
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(
            String name, String category, String sellerId,
            BigDecimal minPrice, BigDecimal maxPrice) {
        return productRepository.search(name, category, sellerId, minPrice, maxPrice)
                .stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> browseProducts() {
        return productRepository.findByStatus(ProductStatus.APPROVED)
                .stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getSellerProducts(String sellerId) {
        return productRepository.findBySellerId(sellerId)
                .stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    public ProductResponse updateProduct(String id, String sellerId, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        if (!product.getSellerId().equals(sellerId)) {
            throw new RuntimeException("You can only update your own products");
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setBasePrice(request.getBasePrice());

        product = productRepository.save(product);
        return ProductResponse.from(product);
    }

    public void deleteProduct(String id, String sellerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        if (!product.getSellerId().equals(sellerId)) {
            throw new RuntimeException("You can only delete your own products");
        }

        // Per SRS US-006: product tied to an active deal cannot be deleted
        // This check will need to be implemented when Deal Service is available
        // For now, only block deletion if status is not REJECTED (placeholder for active deal check)
        if (product.getStatus() == ProductStatus.PENDING) {
            throw new RuntimeException("Cannot delete a product that is pending admin review");
        }

        productRepository.delete(product);
    }

    public ProductResponse approveProduct(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        if (product.getStatus() != ProductStatus.PENDING) {
            throw new RuntimeException("Only pending products can be approved");
        }

        product.setStatus(ProductStatus.APPROVED);
        product = productRepository.save(product);
        return ProductResponse.from(product);
    }

    public ProductResponse rejectProduct(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        if (product.getStatus() != ProductStatus.PENDING) {
            throw new RuntimeException("Only pending products can be rejected");
        }

        product.setStatus(ProductStatus.REJECTED);
        product = productRepository.save(product);
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getPendingProducts() {
        return productRepository.findByStatus(ProductStatus.PENDING)
                .stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }
}
