package com.rally.catalog.repository;

import com.rally.catalog.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, String>, JpaSpecificationExecutor<Product> {

    long countByCategoryId(String categoryId);

    @Query("SELECT p.category.id, COUNT(p) FROM Product p WHERE p.deletedAt IS NULL GROUP BY p.category.id")
    List<Object[]> countProductsGroupedByCategory();

    List<Product> findAllBySellerIdAndDeletedAtIsNull(UUID sellerId);
}
