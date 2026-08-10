export type RootSurface = 'landing' | 'storefront'

export interface HostRoutingConfig {
  rootDomains: readonly string[]
  rootAliases: readonly string[]
  reservedLabels?: readonly string[]
}

const DEFAULT_RESERVED_LABELS = ['www', 'app']
const TENANT_LABEL = /^(?!-)[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$/

function normalizeHost(hostname: string): string {
  return hostname.trim().toLowerCase().replace(/^\[|\]$/g, '').replace(/\.$/, '')
}

function configuredHosts(value: string | undefined): string[] {
  return (value ?? '').split(',').map(normalizeHost).filter(Boolean)
}

export const hostRoutingConfig: HostRoutingConfig = {
  rootDomains: configuredHosts(import.meta.env.VITE_ROOT_DOMAINS),
  rootAliases: configuredHosts(import.meta.env.VITE_ROOT_HOST_ALIASES),
}

export function selectRootSurface(hostname: string, config: HostRoutingConfig): RootSurface {
  const host = normalizeHost(hostname)
  if (!host || host === 'localhost' || host === '::1' || /^127(?:\.\d{1,3}){3}$/.test(host)) return 'landing'

  const roots = config.rootDomains.map(normalizeHost)
  const aliases = config.rootAliases.map(normalizeHost)
  if (roots.includes(host) || aliases.includes(host)) return 'landing'

  const reserved = new Set([...(config.reservedLabels ?? DEFAULT_RESERVED_LABELS), ...DEFAULT_RESERVED_LABELS])
  for (const root of roots) {
    const suffix = `.${root}`
    if (!host.endsWith(suffix)) continue
    const label = host.slice(0, -suffix.length)
    return TENANT_LABEL.test(label) && !reserved.has(label) ? 'storefront' : 'landing'
  }
  return 'landing'
}

export function hasTenantSubdomain(
  hostname: string = typeof window !== 'undefined' ? window.location.hostname : '',
  search: string = typeof window !== 'undefined' ? window.location.search : '',
  config: HostRoutingConfig = hostRoutingConfig,
  isDevelopment: boolean = import.meta.env.DEV,
): boolean {
  if (isDevelopment && new URLSearchParams(search).has('storefront')) return true
  return selectRootSurface(hostname, config) === 'storefront'
}

/** Cart namespacing only. Tenant authorization and isolation remain server-side. */
export function getStorefrontSlug(
  hostname: string = typeof window !== 'undefined' ? window.location.hostname : '',
  config: HostRoutingConfig = hostRoutingConfig,
): string {
  const host = normalizeHost(hostname)
  for (const root of config.rootDomains.map(normalizeHost)) {
    const suffix = `.${root}`
    if (host.endsWith(suffix)) return host.slice(0, -suffix.length)
  }
  return host
}
