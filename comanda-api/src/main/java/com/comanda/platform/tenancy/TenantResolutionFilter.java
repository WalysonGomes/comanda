package com.comanda.platform.tenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Populates {@link TenantContext} before any data access (D1, D2). Owner-panel API routes
 * ({@code /api/painel/**}) resolve by JWT; every other {@code /api/**} route resolves by
 * subdomain, except the public auth routes ({@code /api/auth/**} — signup/login/refresh from
 * {@code owner-auth}) and the operator-only admin routes ({@code /api/admin/**} — manual plan
 * activation, task 4.1), neither of which has a single tenant to resolve, so both skip resolution
 * entirely. Non-API routes (SPA, static assets) also skip resolution. A request whose tenant
 * can't be resolved never reaches a handler.
 */
@Component
@Order(1)
public class TenantResolutionFilter extends OncePerRequestFilter {

    private static final String API_PREFIX = "/api/";
    private static final String OWNER_PANEL_PREFIX = "/api/painel/";
    private static final String AUTH_PREFIX = "/api/auth/";
    private static final String ADMIN_PREFIX = "/api/admin/";

    private final SubdomainTenantResolver subdomainTenantResolver;
    private final JwtTenantResolver jwtTenantResolver;

    public TenantResolutionFilter(SubdomainTenantResolver subdomainTenantResolver, JwtTenantResolver jwtTenantResolver) {
        this.subdomainTenantResolver = subdomainTenantResolver;
        this.jwtTenantResolver = jwtTenantResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();

        if (!path.startsWith(API_PREFIX) || path.startsWith(AUTH_PREFIX) || path.startsWith(ADMIN_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            boolean isOwnerPanelRoute = path.startsWith(OWNER_PANEL_PREFIX);
            TenantResolver resolver = isOwnerPanelRoute ? jwtTenantResolver : subdomainTenantResolver;
            Optional<Long> tenantId = resolver.resolve(request);

            if (tenantId.isEmpty()) {
                response.sendError(isOwnerPanelRoute ? HttpServletResponse.SC_UNAUTHORIZED : HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            TenantContext.set(tenantId.get());
            if (isOwnerPanelRoute) {
                jwtTenantResolver.resolveUserId(request).ifPresent(UserContext::set);
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            UserContext.clear();
        }
    }
}
