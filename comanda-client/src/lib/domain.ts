export const RESERVED_TENANT_LABELS = ['www', 'app', 'api', 'docs', 'status', 'admin', 'demo', 'signal'] as const

export const canonicalTenantDomain = (import.meta.env.VITE_TENANT_DOMAIN || import.meta.env.VITE_ROOT_DOMAINS?.split(',')[0] || '').trim().toLowerCase()
export const tenantDomainSuffix = `.${canonicalTenantDomain}`

export function tenantMenuHost(subdomain: string): string {
  return `${subdomain.trim().toLowerCase()}${tenantDomainSuffix}`
}

export function normalizeTenantLabel(value: string): string {
  return value.toLowerCase().replace(/[^a-z0-9-]/g, '-').replace(/-{2,}/g, '-').replace(/^-|-$/g, '')
}

export function isReservedTenantLabel(value: string): boolean {
  return RESERVED_TENANT_LABELS.includes(normalizeTenantLabel(value) as (typeof RESERVED_TENANT_LABELS)[number])
}
