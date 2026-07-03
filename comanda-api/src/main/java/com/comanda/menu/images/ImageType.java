package com.comanda.menu.images;

/** The only three formats accepted for product photos (design.md Decision 2 / PRD 4.5). */
public enum ImageType {
    JPEG("jpg"),
    PNG("png"),
    WEBP("webp");

    private final String extension;

    ImageType(String extension) {
        this.extension = extension;
    }

    public String extension() {
        return extension;
    }
}
