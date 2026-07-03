package com.comanda.platform.tenancy;

/**
 * Holder for the tenant resolved for the current request. Populated by
 * {@link TenantResolutionFilter} before any data access, cleared at the end of the request.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long tenantId) {
        CURRENT_TENANT_ID.set(tenantId);
    }

    public static Long get() {
        return CURRENT_TENANT_ID.get();
    }

    public static boolean isResolved() {
        return CURRENT_TENANT_ID.get() != null;
    }

    public static void clear() {
        CURRENT_TENANT_ID.remove();
    }
}
