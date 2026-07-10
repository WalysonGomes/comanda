package com.comanda.platform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gates {@code /api/admin/**} (task 4.1, PRD Seção 10): a static bearer token configured out of
 * band (env var), never the OWNER's JWT — {@link com.comanda.platform.tenancy.TenantResolutionFilter}
 * excludes this prefix from tenant/JWT resolution entirely, so there is no path from an OWNER's
 * own session to a plan-elevating endpoint. Cheapest option that satisfies "restrito ao operador"
 * without a login screen (design.md Open Question, resolved as endpoint + credential).
 */
@Component
@Order(0)
public class AdminAuthFilter extends OncePerRequestFilter {

    private static final String ADMIN_PREFIX = "/api/admin/";

    private final String adminToken;

    public AdminAuthFilter(@Value("${app.admin.token:}") String adminToken) {
        this.adminToken = adminToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith(ADMIN_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String expected = "Bearer " + adminToken;
        String provided = request.getHeader("Authorization");
        if (adminToken.isBlank() || provided == null || !constantTimeEquals(expected, provided)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Credencial de operador inválida.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean constantTimeEquals(String a, String b) {
        return java.security.MessageDigest.isEqual(
                a.getBytes(java.nio.charset.StandardCharsets.UTF_8), b.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
