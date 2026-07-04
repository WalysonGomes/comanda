const APP_DOMAIN = 'comanda.local'

/**
 * Only used to namespace the cart's `localStorage` key (design.md Decision 4) so two businesses
 * never share a cart — tenant *security* is enforced server-side from the real `Host` header
 * (spec `multi-tenancy`), never from this. Falls back to the raw hostname outside `$APP_DOMAIN`
 * (e.g. `localhost` in dev), which is still a stable per-host key.
 */
export function getStorefrontSlug(hostname: string = window.location.hostname): string {
  const suffix = `.${APP_DOMAIN}`
  return hostname.endsWith(suffix) ? hostname.slice(0, -suffix.length) : hostname
}
