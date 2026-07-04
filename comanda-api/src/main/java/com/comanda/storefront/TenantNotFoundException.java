package com.comanda.storefront;

/**
 * Defensive only: {@link com.comanda.platform.tenancy.TenantResolutionFilter} already rejects a
 * request whose subdomain doesn't resolve before it reaches any controller, so this should never
 * actually be thrown in production — kept so a resolved-but-since-deleted tenant still fails
 * loudly (Regra 11) instead of a stack trace leaking past a missing {@code .orElseThrow}.
 */
public class TenantNotFoundException extends RuntimeException {
}
