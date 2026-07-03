package com.comanda.platform.tenancy;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * Resolves the current tenant id from an incoming request. One implementation per route type
 * (D2): {@link SubdomainTenantResolver} for the public storefront, {@link JwtTenantResolver} for
 * the authenticated owner panel.
 */
public interface TenantResolver {

    Optional<Long> resolve(HttpServletRequest request);
}
