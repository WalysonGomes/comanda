package com.comanda.storefront.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Not a {@link com.comanda.platform.tenancy.TenantScopedRepository}: {@link StorefrontTenant}'s id
 * IS the tenant id (it maps {@code tenants} itself), so there's no separate {@code tenant_id}
 * column to filter by — callers look up by {@code TenantContext.get()} directly.
 */
public interface StorefrontTenantRepository extends JpaRepository<StorefrontTenant, Long> {
}
