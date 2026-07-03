package com.comanda.menu.images.web;

import com.comanda.menu.UnsupportedImageTypeException;
import com.comanda.menu.images.ImageStorage;
import com.comanda.menu.images.ImageType;
import com.comanda.menu.images.ImageTypeDetector;
import com.comanda.platform.tenancy.TenantContext;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Upload is a step separate from saving the product (design.md Decision 2): a failure here never
 * blocks the product from being saved; the front-end saves without a photo and offers retry.
 */
@RestController
@RequestMapping("/api/painel/images")
public class ImageUploadController {

    private final ImageStorage imageStorage;

    public ImageUploadController(ImageStorage imageStorage) {
        this.imageStorage = imageStorage;
    }

    @PostMapping
    public ResponseEntity<ImageUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        ImageType type = ImageTypeDetector.detect(content).orElseThrow(UnsupportedImageTypeException::new);
        ImageStorage.StoredImage stored = imageStorage.store(content, type, TenantContext.get());
        return ResponseEntity.status(HttpStatus.CREATED).body(new ImageUploadResponse(stored.imageUrl()));
    }
}
