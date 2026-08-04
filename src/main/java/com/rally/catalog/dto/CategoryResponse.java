package com.rally.catalog.dto;

import com.rally.catalog.entity.Category;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CategoryResponse {

    private String id;
    private String name;
    private String description;
    private LocalDateTime createdAt;

    public static CategoryResponse from(Category category) {
        CategoryResponse res = new CategoryResponse();
        res.id = category.getId();
        res.name = category.getName();
        res.description = category.getDescription();
        res.createdAt = category.getCreatedAt();
        return res;
    }
}
