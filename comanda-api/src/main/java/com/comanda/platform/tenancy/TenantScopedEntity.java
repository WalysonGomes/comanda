package com.comanda.platform.tenancy;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

/**
 * Base class for every tenant-scoped entity. Declares the {@code tenantFilter} Hibernate filter
 * (D1); {@link TenantFilterInterceptor} enables it per request with the current tenant id, so
 * every query against a subclass is restricted transparently.
 */
@MappedSuperclass
@FilterDef(name = TenantScopedEntity.FILTER_NAME, parameters = @ParamDef(name = "tenantId", type = Long.class))
@Filter(name = TenantScopedEntity.FILTER_NAME, condition = "tenant_id = :tenantId")
public abstract class TenantScopedEntity {

    public static final String FILTER_NAME = "tenantFilter";

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    protected TenantScopedEntity() {
    }

    protected TenantScopedEntity(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getTenantId() {
        return tenantId;
    }
}
