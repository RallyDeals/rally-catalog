package com.rally.catalog.repository;

import com.rally.catalog.entity.ProductActiveDeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductActiveDealRepository extends JpaRepository<ProductActiveDeal, String> {

    List<ProductActiveDeal> findByProductIdAndStatusIn(String productId, List<String> statuses);

    boolean existsByProductIdAndStatusIn(String productId, List<String> statuses);

    List<ProductActiveDeal> findByProductIdInAndStatusIn(List<String> productIds, List<String> statuses);

    void deleteByProductId(String productId);
}
