const APP_DOMAIN = 'comanda.local'

/**
 * Checks if the current hostname corresponds to a tenant subdomain.
 * Returns true if on a tenant subdomain (e.g. meunegocio.comanda.local) or if ?storefront parameter is set.
 */
export function hasTenantSubdomain(hostname: string = typeof window !== 'undefined' ? window.location.hostname : ''): boolean {
  if (typeof window !== 'undefined' && window.location.search.includes('storefront')) {
    return true
  }
  const suffix = `.${APP_DOMAIN}`
  if (hostname.endsWith(suffix)) {
    const sub = hostname.slice(0, -suffix.length)
    return sub.length > 0 && sub !== 'www' && sub !== 'app'
  }
  const parts = hostname.split('.')
  if (
    parts.length > 1 &&
    parts[0] !== 'www' &&
    parts[0] !== 'app' &&
    parts[0] !== 'comanda' &&
    hostname !== 'localhost' &&
    !/^\d+\.\d+\.\d+\.\d+$/.test(hostname)
  ) {
    return true
  }
  return false
}

/**
 * Only used to namespace the cart's `localStorage` key (design.md Decision 4) so two businesses
 * never share a cart — tenant *security* is enforced server-side from the real `Host` header
 * (spec `multi-tenancy`), never from this. Falls back to the raw hostname outside `$APP_DOMAIN`
 * (e.g. `localhost` in dev), which is still a stable per-host key.
 */
export function getStorefrontSlug(hostname: string = typeof window !== 'undefined' ? window.location.hostname : ''): string {
  const suffix = `.${APP_DOMAIN}`
  return hostname.endsWith(suffix) ? hostname.slice(0, -suffix.length) : hostname
}
