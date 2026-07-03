package com.comanda.auth;

import java.util.Locale;

/**
 * Normalizes a user-typed subdomain to the single-label, lowercase form
 * {@code SubdomainTenantResolver} matches against (D2).
 */
public final class SubdomainNormalizer {

    private SubdomainNormalizer() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String slug = raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-");
        slug = slug.replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
        return slug;
    }
}
