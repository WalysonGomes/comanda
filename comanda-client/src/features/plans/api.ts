const PAINEL_BASE = '/api/painel'

export class PlansApiError extends Error {
  code: string
  status: number

  constructor(status: number, code: string, message: string) {
    super(message)
    this.code = code
    this.status = status
  }
}

export type PlanStatus = {
  businessName: string
  subdomain: string
  menuUrl: string
  plan: 'GRATUITO' | 'ESSENCIAL'
  orderCountMonth: number
  orderLimit: number | null
  showQuotaWarning: boolean
  productCount: number
  productLimit: number | null
  categoryCount: number
  categoryLimit: number | null
}

async function request<T>(accessToken: string, path: string): Promise<T> {
  const response = await fetch(`${PAINEL_BASE}${path}`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  })
  const data = await response.json().catch(() => null)
  if (!response.ok) {
    throw new PlansApiError(response.status, data?.code ?? 'UNKNOWN_ERROR', data?.message ?? 'Algo deu errado. Tente novamente.')
  }
  return data as T
}

export const plansApi = {
  status: (token: string) => request<PlanStatus>(token, '/plans/status'),
}
