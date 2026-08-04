package com.groupdeal.catalog.repository;

import com.groupdeal.catalog.entity.Product;
import com.groupdeal.catalog.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    @Query(value = """
            SELECT * FROM products p
            WHERE p.status = 'APPROVED'
              AND p.deleted_at IS NULL
              AND to_tsvector('english', p.name || ' ' || p.description) @@ plainto_tsquery('english', :q)
              AND (:categoryId IS NULL OR p.category_id::text = :categoryId)
              AND (:sellerId IS NULL OR p.seller_id::text = :sellerId)
              AND (:minPrice IS NULL OR p.base_price >= :minPrice)
              AND (:maxPrice IS NULL OR p.base_price <= :maxPrice)
            """,
            countQuery = """
            SELECT COUNT(*) FROM products p
            WHERE p.status = 'APPROVED'
              AND p.deleted_at IS NULL
              AND to_tsvector('english', p.name || ' ' || p.description) @@ plainto_tsquery('english', :q)
              AND (:categoryId IS NULL OR p.category_id::text = :categoryId)
              AND (:sellerId IS NULL OR p.seller_id::text = :sellerId)
              AND (:minPrice IS NULL OR p.base_price >= :minPrice)
              AND (:maxPrice IS NULL OR p.base_price <= :maxPrice)
            """)
    Page<Product> search(
            @Param("q") String q,
            @Param("categoryId") String categoryId,
            @Param("sellerId") String sellerId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            WHERE p.status = 'APPROVED'
              AND p.deletedAt IS NULL
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (:sellerId IS NULL OR p.sellerId = :sellerId)
              AND (:minPrice IS NULL OR p.basePrice >= :minPrice)
              AND (:maxPrice IS NULL OR p.basePrice <= :maxPrice)
            """,
            countQuery = """
            SELECT COUNT(p) FROM Product p
            WHERE p.status = 'APPROVED'
              AND p.deletedAt IS NULL
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (:sellerId IS NULL OR p.sellerId = :sellerId)
              AND (:minPrice IS NULL OR p.basePrice >= :minPrice)
              AND (:maxPrice IS NULL OR p.basePrice <= :maxPrice)
            """)
    Page<Product> browse(
            @Param("categoryId") String categoryId,
            @Param("sellerId") String sellerId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            WHERE p.sellerId = :sellerId
              AND (:status IS NULL OR p.status = :status)
              AND (:includeDeleted = true OR p.deletedAt IS NULL)
            """,
            countQuery = """
            SELECT COUNT(p) FROM Product p
            WHERE p.sellerId = :sellerId
              AND (:status IS NULL OR p.status = :status)
              AND (:includeDeleted = true OR p.deletedAt IS NULL)
            """)
    Page<Product> findBySeller(
            @Param("sellerId") String sellerId,
            @Param("status") ProductStatus status,
            @Param("includeDeleted") boolean includeDeleted,
            Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            WHERE (:status IS NULL OR p.status = :status)
              AND (:includeDeleted = true OR p.deletedAt IS NULL)
            """,
            countQuery = """
            SELECT COUNT(p) FROM Product p
            WHERE (:status IS NULL OR p.status = :status)
              AND (:includeDeleted = true OR p.deletedAt IS NULL)
            """)
    Page<Product> findAllForAdmin(
            @Param("status") ProductStatus status,
            @Param("includeDeleted") boolean includeDeleted,
            Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.id IN :ids AND p.status = 'APPROVED' AND p.deletedAt IS NULL")
    List<Product> findApprovedByIds(@Param("ids") Collection<String> ids);

    long countByCategoryId(String categoryId);
}
