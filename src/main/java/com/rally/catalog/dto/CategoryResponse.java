package com.rally.catalog.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class CategoryResponse {

    private UUID id;
    private String name;
    private String description;
    private String icon;
    private int productsCount;
    private LocalDateTime createdAt;
}
