package com.comanda.platform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limits login/refresh (task 2.4, PRD Seção 9) to blunt brute force: a token bucket keyed by
 * client IP + route. Response is a generic 429 in the same {@code {code, message}} shape as
 * controller-thrown errors (see the per-module {@code @RestControllerAdvice} classes) — it never
 * reveals whether the credentials would have been valid, keeping it consistent with the
 * non-enumeration guarantee in {@link com.comanda.auth.AuthenticationService}.
 *
 * <p>Ordered ahead of {@link com.comanda.platform.tenancy.TenantResolutionFilter} so a limited
 * client is rejected before any other processing, though the two paths this filter watches are
 * already excluded from tenant resolution.
 */
@Component
@Order(0)
public class AuthRateLimitingFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of("/api/auth/login", "/api/auth/refresh");

    private final InMemoryRateLimiter rateLimiter;

    public AuthRateLimitingFilter(
            @Value("${app.rate-limit.auth.capacity:10}") long capacity,
            @Value("${app.rate-limit.auth.window-seconds:60}") long windowSeconds) {
        this.rateLimiter = new InMemoryRateLimiter(capacity, windowSeconds * 1000);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!LIMITED_PATHS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = request.getRemoteAddr() + ":" + path;
        if (!rateLimiter.tryConsume(key)) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter()
                    .write("{\"code\":\"RATE_LIMITED\",\"message\":\"Muitas tentativas. Tente novamente em instantes.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
