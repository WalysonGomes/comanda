import { createBrowserRouter } from 'react-router'

import { LoginPage } from '@/features/auth/LoginPage'
import { RequireAuth } from '@/features/auth/RequireAuth'
import { SignupPage } from '@/features/auth/SignupPage'
import { PainelShell } from '@/features/painel/PainelShell'
import { StorefrontShell } from '@/features/storefront/StorefrontShell'

/**
 * Storefront publico e painel do dono no mesmo bundle SPA, separados por rota (D5 / spec
 * `design-system`). `/painel/*` exige sessao (owner-auth); shell segue vazio, telas reais
 * entram nas changes seguintes.
 */
export const router = createBrowserRouter([
  {
    path: '/',
    element: <StorefrontShell />,
  },
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/cadastro',
    element: <SignupPage />,
  },
  {
    path: '/painel/*',
    element: (
      <RequireAuth>
        <PainelShell />
      </RequireAuth>
    ),
  },
])
