package com.comanda.platform.tenancy;

/**
 * Holder for the OWNER user id resolved from the JWT on owner-panel routes (complement to
 * {@link TenantContext}). Populated by {@link TenantResolutionFilter} alongside the tenant,
 * cleared at the end of the request. Empty on public (subdomain-resolved) routes, where actions
 * are attributed to {@code SYSTEM} instead.
 */
public final class UserContext {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static Long get() {
        return CURRENT_USER_ID.get();
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}
