package com.comanda.platform.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code owner-pwa} (task 6.1): Spring's built-in extension-to-{@code Content-Type} mapping has
 * no entry for {@code .webmanifest}, so the file would otherwise fall back to a generic
 * octet-stream and fail browser install checks. A dedicated endpoint pins the correct MIME type;
 * {@code no-cache} keeps it consistent with {@code /sw.js} (D6) so a redeploy that changes icons
 * or scope is picked up on next load, not frozen behind a stale cached copy.
 */
@RestController
public class ManifestController {

    private static final MediaType MANIFEST_TYPE = MediaType.parseMediaType("application/manifest+json");

    @GetMapping(value = "/manifest.webmanifest")
    public ResponseEntity<Resource> manifest() {
        Resource manifest = new ClassPathResource("static/manifest.webmanifest");
        return ResponseEntity.ok()
                .contentType(MANIFEST_TYPE)
                .cacheControl(CacheControl.noCache())
                .body(manifest);
    }
}
