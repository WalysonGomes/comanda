package com.comanda.auth;

import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Canonical, authoritative policy for tenant labels. */
@Component
public class TenantSubdomainPolicy {

    public static final Set<String> RESERVED = Set.of(
            "www", "app", "api", "docs", "status", "admin", "demo", "signal");
    private static final Pattern SYNTAX = Pattern.compile("^(?!-)[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$");

    public String validateAndNormalize(String raw) {
        String normalized = SubdomainNormalizer.normalize(raw);
        if (!SYNTAX.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Subdomínio inválido.");
        }
        if (RESERVED.contains(normalized)) {
            throw new IllegalArgumentException("Este subdomínio é reservado.");
        }
        return normalized;
    }

    public boolean isReserved(String raw) {
        return RESERVED.contains(SubdomainNormalizer.normalize(raw));
    }
}
