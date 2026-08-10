import type { BusinessHoursRow } from '@/features/onboarding/api'

export type BusinessSettings = { businessName: string; logoUrl: string | null; whatsappNumber: string; deliveryFee: string; minOrderValue: string; subdomain: string; businessHours: BusinessHoursRow[] }
export class BusinessSettingsApiError extends Error {
  status: number
  code: string
  constructor(status: number, code: string, message: string) { super(message); this.status = status; this.code = code }
}

async function request(token: string, init?: RequestInit): Promise<BusinessSettings> {
  const headers = new Headers(init?.headers)
  headers.set('Authorization', `Bearer ${token}`)
  if (init?.body && !(init.body instanceof FormData)) headers.set('Content-Type', 'application/json')
  const response = await fetch('/api/painel/business-settings' + (init?.body instanceof FormData ? '/logo' : ''), { ...init, headers })
  const data = await response.json().catch(() => null)
  if (!response.ok) throw new BusinessSettingsApiError(response.status, data?.code ?? 'UNKNOWN_ERROR', data?.message ?? 'Não foi possível salvar.')
  return data
}
export const businessSettingsApi = {
  get: (token: string) => request(token),
  update: (token: string, value: BusinessSettings) => request(token, { method: 'PUT', body: JSON.stringify(value) }),
  logo: (token: string, file: File) => { const body = new FormData(); body.append('file', file); return request(token, { method: 'POST', body }) },
}
