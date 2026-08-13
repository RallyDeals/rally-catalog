package com.rally.catalog.repository;

import com.rally.catalog.entity.Product;
import com.rally.catalog.entity.ProductStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Query building with the JPA Criteria API (Hibernate generates the SQL from
 * these predicates — no SQL/JPQL strings are written directly).
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> approvedAndNotDeleted() {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), ProductStatus.APPROVED),
                cb.isNull(root.get("deletedAt")));
    }

    public static Specification<Product> keyword(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String pattern = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            Predicate nameMatch = cb.like(cb.lower(root.<String>get("name")), pattern);
            Predicate descriptionMatch = cb.like(cb.lower(root.<String>get("description")), pattern);
            Subquery<Product> tagSubquery = query.subquery(Product.class);
            Root<Product> tagRoot = tagSubquery.from(Product.class);
            tagSubquery.select(tagRoot).distinct(true);
            tagSubquery.where(cb.and(
                    cb.equal(tagRoot.get("id"), root.get("id")),
                    cb.like(cb.lower(tagRoot.join("tags")), pattern)));
            return cb.or(nameMatch, descriptionMatch, cb.exists(tagSubquery));
        };
    }

    public static Specification<Product> visible(boolean visible) {
        return (root, query, cb) -> cb.equal(root.get("visible"), visible);
    }

    public static Specification<Product> categoryIs(String categoryId) {
        return (root, query, cb) -> categoryId == null
                ? null
                : cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> sellerIs(String sellerId) {
        return (root, query, cb) -> sellerId == null
                ? null
                : cb.equal(root.get("sellerId"), sellerId);
    }

    public static Specification<Product> priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("basePrice"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("basePrice"), maxPrice));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Product> statusIs(ProductStatus status) {
        return (root, query, cb) -> status == null
                ? null
                : cb.equal(root.get("status"), status);
    }

    public static Specification<Product> notDeleted(boolean includeDeleted) {
        return (root, query, cb) -> includeDeleted
                ? null
                : cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Product> idIn(Collection<String> ids) {
        return (root, query, cb) -> root.get("id").in(ids);
    }
}
