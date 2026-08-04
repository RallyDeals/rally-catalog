package com.groupdeal.catalog.repository;

import com.groupdeal.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {

    boolean existsByNameIgnoreCase(String name);
}
