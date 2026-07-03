package com.comanda.menu.images;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * MVP storage: one directory per tenant under a configurable base path (PRD 7.3). {@code
 * imageUrl} is the {@code /media/**} path {@link com.comanda.platform.web.WebMvcConfig} serves,
 * so callers never see the filesystem layout.
 */
@Component
public class LocalDiskImageStorage implements ImageStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalDiskImageStorage.class);
    private static final String MEDIA_PREFIX = "/media/";

    private final Path baseDir;

    public LocalDiskImageStorage(@Value("${app.storage.images-dir}") String imagesDir) {
        this.baseDir = Path.of(imagesDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public StoredImage store(byte[] content, ImageType type, Long tenantId) {
        try {
            Path tenantDir = baseDir.resolve(String.valueOf(tenantId));
            Files.createDirectories(tenantDir);
            String filename = UUID.randomUUID() + "." + type.extension();
            Files.write(tenantDir.resolve(filename), content);
            return new StoredImage(MEDIA_PREFIX + tenantId + "/" + filename);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void delete(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(MEDIA_PREFIX)) {
            return;
        }
        try {
            Path path = baseDir.resolve(imageUrl.substring(MEDIA_PREFIX.length())).normalize();
            if (!path.startsWith(baseDir)) {
                return;
            }
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Falha ao remover imagem antiga (best-effort, sem bloquear a operação): {}", imageUrl);
        }
    }

    public Path getBaseDir() {
        return baseDir;
    }
}
