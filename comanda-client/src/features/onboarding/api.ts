const PAINEL_BASE = '/api/painel/onboarding'

export class OnboardingApiError extends Error {
  code: string
  status: number

  constructor(status: number, code: string, message: string) {
    super(message)
    this.code = code
    this.status = status
  }
}

export type Segment = 'MARMITARIA' | 'CONFEITARIA' | 'HAMBURGUERIA' | 'ACAIZERIA'

export type BusinessHoursRow = {
  dayOfWeek: number
  opensAt: string | null
  closesAt: string | null
  closed: boolean
}

async function request<T>(accessToken: string, path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.body ? { 'Content-Type': 'application/json' } : undefined)
  headers.set('Authorization', `Bearer ${accessToken}`)

  const response = await fetch(`${PAINEL_BASE}${path}`, { ...init, headers })
  if (response.status === 204) {
    return undefined as T
  }
  const data = await response.json().catch(() => null)
  if (!response.ok) {
    throw new OnboardingApiError(response.status, data?.code ?? 'UNKNOWN_ERROR', data?.message ?? 'Algo deu errado. Tente novamente.')
  }
  return data as T
}

export const onboardingApi = {
  seed: (token: string, segment: Segment) =>
    request<void>(token, '/seed', { method: 'POST', body: JSON.stringify({ segment }) }),
  saveBusinessHours: (token: string, rows: BusinessHoursRow[]) =>
    request<void>(token, '/business-hours', { method: 'PUT', body: JSON.stringify({ rows }) }),
}

export function defaultBusinessHours(): BusinessHoursRow[] {
  return Array.from({ length: 7 }, (_, dayOfWeek) => ({
    dayOfWeek,
    opensAt: dayOfWeek === 0 ? null : '08:00',
    closesAt: dayOfWeek === 0 ? null : '18:00',
    closed: dayOfWeek === 0,
  }))
}
