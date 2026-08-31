package com.rally.catalog.repository;

import com.rally.catalog.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    long countByCategoryId(UUID categoryId);

    @Query("SELECT p.category.id, COUNT(p) FROM Product p WHERE p.deletedAt IS NULL GROUP BY p.category.id")
    List<Object[]> countProductsGroupedByCategory();

    @Modifying
    @Query("UPDATE Product p SET p.visible = false, p.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE p.sellerId = :sellerId AND p.deletedAt IS NULL")
    int setAllInvisibleBySellerId(@Param("sellerId") UUID sellerId);

    List<Product> findByIdIn(List<UUID> ids);

    @Query("""
                SELECT p.sellerId,
                       COUNT(p),
                       SUM(CASE WHEN p.status = ProductStatus.PENDING_APPROVAL THEN 1 ELSE 0 END)
                FROM Product p
                WHERE p.sellerId IN :sellerIds
                  AND p.deletedAt IS NULL
                GROUP BY p.sellerId
            """)
    List<Object[]> getSellerProductsInfo(@Param("sellerIds") List<UUID> sellerIds);
}
