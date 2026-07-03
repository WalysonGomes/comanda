package com.comanda.auth.security;

import com.comanda.auth.domain.User;
import com.comanda.platform.tenancy.JwtTenantResolver;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Issues and validates the two JWTs of a session (D4, D5). Access tokens carry the
 * {@value JwtTenantResolver#TENANT_CLAIM} claim in the exact format
 * {@link JwtTenantResolver} already reads; refresh tokens are opaque-to-the-client but carry a
 * {@code jti} so each one is unique even for the same user issued in the same second.
 */
@Component
public class TokenService {

    private final SecretKey signingKey;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public TokenService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-ttl-minutes}") long accessTtlMinutes,
            @Value("${app.jwt.refresh-ttl-days}") long refreshTtlDays) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = Duration.ofMinutes(accessTtlMinutes);
        this.refreshTtl = Duration.ofDays(refreshTtlDays);
    }

    public String issueAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(JwtTenantResolver.TENANT_CLAIM, user.getTenantId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(signingKey)
                .compact();
    }

    public String issueRefreshToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("jti", UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTtl)))
                .signWith(signingKey)
                .compact();
    }

    public Optional<Long> validateRefreshTokenAndGetUserId(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
            return Optional.of(Long.valueOf(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Duration refreshTtl() {
        return refreshTtl;
    }
}
