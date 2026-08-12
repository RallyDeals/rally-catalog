package com.rally.catalog.service;

import com.rally.common.exceptions.shared.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class ImageStorageService {

    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024L;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/svg+xml", "image/png", "image/jpeg");
    private static final String UPLOAD_PREFIX = "/uploads";

    private final Path productsDir;

    public ImageStorageService(@Value("${app.storage.root:./uploads}") String storageRoot) {
        this.productsDir = Path.of(storageRoot).toAbsolutePath().normalize().resolve("products");
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BadRequestException("Unsupported file type. Use SVG, PNG, or JPG.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BadRequestException("Image too large. Maximum size is 5MB.");
        }

        String filename = UUID.randomUUID() + extensionFor(contentType);
        try {
            Files.createDirectories(productsDir);
            Files.copy(file.getInputStream(), productsDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store image", e);
        }
        return UPLOAD_PREFIX + "/products/" + filename;
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/svg+xml" -> ".svg";
            default -> throw new BadRequestException("Unsupported file type. Use SVG, PNG, or JPG.");
        };
    }
}
