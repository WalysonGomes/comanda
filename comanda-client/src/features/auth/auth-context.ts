import { createContext, use } from 'react'

import type { AuthResponse, LoginPayload, SignupPayload, UserSummary } from '@/lib/api'

export type AuthStatus = 'loading' | 'authenticated' | 'anonymous'

export type AuthContextValue = {
  status: AuthStatus
  user: UserSummary | null
  accessToken: string | null
  login: (payload: LoginPayload) => Promise<void>
  // Returns the response (not just void) so onboarding can chain the seed/business-hours calls
  // with the fresh token immediately, without waiting a render for context to catch up.
  signup: (payload: SignupPayload) => Promise<AuthResponse>
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth() {
  const ctx = use(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return ctx
}
