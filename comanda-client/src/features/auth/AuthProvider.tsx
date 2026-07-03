import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'

import { AuthContext, type AuthStatus } from '@/features/auth/auth-context'
import { authApi, type LoginPayload, type SignupPayload, type UserSummary } from '@/lib/api'

/**
 * Access token lives only in memory (D7/spec `owner-auth`): never localStorage/sessionStorage.
 * On boot, a silent refresh (httpOnly cookie) restores the session if one exists.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('loading')
  const [user, setUser] = useState<UserSummary | null>(null)
  const [accessToken, setAccessToken] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    authApi
      .refresh()
      .then((response) => {
        if (cancelled) return
        setAccessToken(response.accessToken)
        setUser(response.user)
        setStatus('authenticated')
      })
      .catch(() => {
        if (cancelled) return
        setStatus('anonymous')
      })
    return () => {
      cancelled = true
    }
  }, [])

  const login = useCallback(async (payload: LoginPayload) => {
    const response = await authApi.login(payload)
    setAccessToken(response.accessToken)
    setUser(response.user)
    setStatus('authenticated')
  }, [])

  const signup = useCallback(async (payload: SignupPayload) => {
    const response = await authApi.signup(payload)
    setAccessToken(response.accessToken)
    setUser(response.user)
    setStatus('authenticated')
  }, [])

  const value = useMemo(
    () => ({ status, user, accessToken, login, signup }),
    [status, user, accessToken, login, signup],
  )

  return <AuthContext value={value}>{children}</AuthContext>
}
