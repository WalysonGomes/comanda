import { LandingPage } from '@/features/landing/LandingPage'
import { StorefrontShell } from '@/features/storefront/StorefrontShell'
import { hasTenantSubdomain } from '@/features/storefront/subdomain'

export function RootRoute() {
  return hasTenantSubdomain() ? <StorefrontShell /> : <LandingPage />
}
