import { describe, expect, it } from 'vitest'
import { isReservedTenantLabel, normalizeTenantLabel } from './domain'

describe('tenant domain policy UX mirror', () => {
  it.each(['www', 'app', 'api', 'docs', 'status', 'admin', 'demo', 'signal', ' API ', 'WwW'])('rejects %s after normalization', value => {
    expect(isReservedTenantLabel(value)).toBe(true)
  })
  it.each(['minha-loja', 'acme2'])('accepts tenant label %s', value => expect(isReservedTenantLabel(value)).toBe(false))
  it('normalizes typed labels consistently', () => expect(normalizeTenantLabel(' Minha Loja ')).toBe('minha-loja'))
})
