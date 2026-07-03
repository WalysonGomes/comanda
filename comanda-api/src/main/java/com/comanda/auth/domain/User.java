package com.comanda.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * The OWNER user for a tenant. Not a {@code TenantScopedEntity}: login resolves by e-mail before
 * any tenant is known, so this entity is queried directly rather than through the Hibernate
 * tenant filter.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "refresh_token_hash", length = 64)
    private String refreshTokenHash;

    @Column(name = "refresh_token_expires_at")
    private OffsetDateTime refreshTokenExpiresAt;

    protected User() {
    }

    public User(Long tenantId, String name, String email, String passwordHash) {
        this.tenantId = tenantId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public void setRefreshToken(String hash, OffsetDateTime expiresAt) {
        this.refreshTokenHash = hash;
        this.refreshTokenExpiresAt = expiresAt;
    }

    public void clearRefreshToken() {
        this.refreshTokenHash = null;
        this.refreshTokenExpiresAt = null;
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActive() {
        return active;
    }

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public OffsetDateTime getRefreshTokenExpiresAt() {
        return refreshTokenExpiresAt;
    }
}
