import { describe, expect, it } from 'vitest'

import { hasTenantSubdomain, selectRootSurface, type HostRoutingConfig } from './subdomain'

const config: HostRoutingConfig = {
  rootDomains: ['comanda.example'],
  rootAliases: ['preview.example.dev', 'local.comanda.test'],
}

describe('root surface host routing', () => {
  it.each([
    ['comanda.example', 'landing'],
    ['www.comanda.example', 'landing'],
    ['app.comanda.example', 'landing'],
    ['preview.example.dev', 'landing'],
    ['local.comanda.test', 'landing'],
    ['localhost', 'landing'],
    ['127.0.0.1', 'landing'],
    ['::1', 'landing'],
    ['acme.comanda.example', 'storefront'],
  ] as const)('%s selects %s', (host, surface) => {
    expect(selectRootSurface(host, config)).toBe(surface)
  })

  it.each(['www', 'app', 'api', 'docs', 'status', 'admin', 'demo', 'signal'])('keeps reserved label %s on landing', (label) => {
    expect(selectRootSurface(`${label}.comanda.example`, config)).toBe('landing')
  })

  it('rejects nested and invalid tenant labels', () => {
    expect(selectRootSurface('one.two.comanda.example', config)).toBe('landing')
    expect(selectRootSurface('-invalid.comanda.example', config)).toBe('landing')
  })

  it('allows the query override only in development', () => {
    expect(hasTenantSubdomain('localhost', '?storefront', config, true)).toBe(true)
    expect(hasTenantSubdomain('localhost', '?storefront', config, false)).toBe(false)
  })
})
