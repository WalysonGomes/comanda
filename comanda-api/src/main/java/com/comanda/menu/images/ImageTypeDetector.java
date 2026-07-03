package com.comanda.menu.images;

import java.util.Optional;

/**
 * Detects the real file type from its leading bytes, never trusting the declared extension or
 * Content-Type (PRD 4.5 / Regra 9, design.md Decision 2): JPEG {@code FF D8 FF}, PNG
 * {@code 89 50 4E 47}, WebP {@code RIFF....WEBP}.
 */
public final class ImageTypeDetector {

    private static final int[] JPEG_MAGIC = {0xFF, 0xD8, 0xFF};
    private static final int[] PNG_MAGIC = {0x89, 0x50, 0x4E, 0x47};

    private ImageTypeDetector() {
    }

    public static Optional<ImageType> detect(byte[] bytes) {
        if (matches(bytes, 0, JPEG_MAGIC)) {
            return Optional.of(ImageType.JPEG);
        }
        if (matches(bytes, 0, PNG_MAGIC)) {
            return Optional.of(ImageType.PNG);
        }
        if (isWebp(bytes)) {
            return Optional.of(ImageType.WEBP);
        }
        return Optional.empty();
    }

    private static boolean isWebp(byte[] bytes) {
        if (bytes.length < 12) {
            return false;
        }
        boolean riff = bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F';
        boolean webp = bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
        return riff && webp;
    }

    private static boolean matches(byte[] bytes, int offset, int[] magic) {
        if (bytes.length < offset + magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if ((bytes[offset + i] & 0xFF) != magic[i]) {
                return false;
            }
        }
        return true;
    }
}
