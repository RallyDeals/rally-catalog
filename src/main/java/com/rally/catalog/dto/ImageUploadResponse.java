package com.rally.catalog.dto;

import lombok.Getter;

@Getter
public class ImageUploadResponse {

    private final String path;

    public ImageUploadResponse(String path) {
        this.path = path;
    }
}
