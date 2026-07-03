package com.comanda.menu.images;

/**
 * Abstracted so the MVP's local-disk implementation can be swapped for an S3-compatible bucket
 * later without touching the domain (PRD 7.3, design.md Decision 2).
 */
public interface ImageStorage {

    StoredImage store(byte[] content, ImageType type, Long tenantId);

    /** Best-effort: a failure here (file already gone, permission issue) never blocks the caller. */
    void delete(String imageUrl);

    record StoredImage(String imageUrl) {
    }
}
