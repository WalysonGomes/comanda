package com.comanda.platform.tenancy;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resolves the tenant from the JWT on authenticated owner-panel routes (D2), ignoring the
 * subdomain. The exact claim shape belongs to {@code owner-auth} (design.md open question); this
 * only defines the injection point: a numeric {@code tenantId} claim on a signature-verified
 * token.
 */
@Component
public class JwtTenantResolver implements TenantResolver {

    public static final String TENANT_CLAIM = "tenantId";
    private static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey signingKey;

    public JwtTenantResolver(@Value("${app.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Optional<Long> resolve(HttpServletRequest request) {
        return parseClaims(request).map(claims -> {
            Object tenantId = claims.get(TENANT_CLAIM);
            return tenantId == null ? null : Long.valueOf(tenantId.toString());
        });
    }

    /**
     * Resolves the OWNER user id from the same access token's subject (D2 complement), used by
     * {@code order-operation} to attribute status changes to a user in {@code
     * order_status_history.changed_by_user_id} rather than {@code SYSTEM}.
     */
    public Optional<Long> resolveUserId(HttpServletRequest request) {
        return parseClaims(request)
                .map(Claims::getSubject)
                .filter(subject -> subject != null && !subject.isBlank())
                .map(Long::valueOf);
    }

    private Optional<Claims> parseClaims(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        String token = header.substring(BEARER_PREFIX.length());
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
