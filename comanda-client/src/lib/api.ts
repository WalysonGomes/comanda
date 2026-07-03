const AUTH_BASE = '/api/auth'

export class ApiError extends Error {
  code: string
  status: number

  constructor(status: number, code: string, message: string) {
    super(message)
    this.code = code
    this.status = status
  }
}

async function post<T>(path: string, body?: unknown): Promise<T> {
  const response = await fetch(`${AUTH_BASE}${path}`, {
    method: 'POST',
    credentials: 'include',
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  })

  const data = await response.json().catch(() => null)

  if (!response.ok) {
    throw new ApiError(response.status, data?.code ?? 'UNKNOWN_ERROR', data?.message ?? 'Algo deu errado. Tente novamente.')
  }

  return data as T
}

export type UserSummary = {
  id: number
  name: string
  email: string
  tenantId: number
}

export type AuthResponse = {
  accessToken: string
  user: UserSummary
}

export type SignupPayload = {
  name: string
  businessName: string
  subdomain: string
  whatsappNumber: string
  email: string
  password: string
}

export type LoginPayload = {
  email: string
  password: string
}

export const authApi = {
  signup: (payload: SignupPayload) => post<AuthResponse>('/signup', payload),
  login: (payload: LoginPayload) => post<AuthResponse>('/login', payload),
  refresh: () => post<AuthResponse>('/refresh'),
}
