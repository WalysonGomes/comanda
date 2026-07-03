import type { ReactNode } from 'react'
import { Navigate } from 'react-router'

import { useAuth } from '@/features/auth/auth-context'

/**
 * Guards the painel shell: no valid session redirects to /login (spec `owner-auth`, "Shell
 * autenticado do painel protegido por sessão").
 */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { status } = useAuth()

  if (status === 'loading') {
    return <div className="min-h-svh bg-cream" />
  }

  if (status !== 'authenticated') {
    return <Navigate to="/login" replace />
  }

  return children
}
