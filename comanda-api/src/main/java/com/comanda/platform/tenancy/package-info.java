/**
 * Tenant isolation mechanism (PRD Regra 9). Convention: tenant-scoped JPA entities extend
 * {@link com.comanda.platform.tenancy.TenantScopedEntity} and their repositories extend
 * {@link com.comanda.platform.tenancy.TenantScopedRepository}, not a plain {@code
 * JpaRepository} — Hibernate's {@code tenantFilter} restricts HQL/JPQL queries once {@link
 * TenantContext} is populated for the request, but not {@code EntityManager.find()} by id, so
 * {@code findById} must go through a filtered query. Never query a tenant-scoped entity without
 * the filter enabled.
 */
package com.comanda.platform.tenancy;
