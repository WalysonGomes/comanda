package com.comanda.platform.tenancy;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Resolves the tenant from the {@code Host} subdomain on public storefront routes (D2):
 * {@code nomedonegocio.$APP_DOMAIN}.
 */
@Component
public class SubdomainTenantResolver implements TenantResolver {

    private final JdbcTemplate jdbcTemplate;
    private final String appDomain;

    public SubdomainTenantResolver(JdbcTemplate jdbcTemplate, @Value("${app.domain}") String appDomain) {
        this.jdbcTemplate = jdbcTemplate;
        this.appDomain = appDomain;
    }

    @Override
    public Optional<Long> resolve(HttpServletRequest request) {
        return extractSubdomain(request.getServerName()).flatMap(this::findTenantIdBySubdomain);
    }

    private Optional<String> extractSubdomain(String host) {
        String suffix = "." + appDomain;
        if (host == null || !host.endsWith(suffix)) {
            return Optional.empty();
        }
        String subdomain = host.substring(0, host.length() - suffix.length());
        if (subdomain.isBlank() || subdomain.contains(".")) {
            return Optional.empty();
        }
        return Optional.of(subdomain);
    }

    private Optional<Long> findTenantIdBySubdomain(String subdomain) {
        return jdbcTemplate.query(
                "SELECT id FROM tenants WHERE subdomain = ?",
                (rs, rowNum) -> rs.getLong("id"),
                subdomain
        ).stream().findFirst();
    }
}
