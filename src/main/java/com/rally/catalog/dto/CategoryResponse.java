package com.rally.catalog.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CategoryResponse {

    private String id;
    private String name;
    private String description;
    private String icon;
    private int productsCount;
    private LocalDateTime createdAt;
}
