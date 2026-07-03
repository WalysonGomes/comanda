import { createBrowserRouter } from 'react-router'

import { StorefrontShell } from '@/features/storefront/StorefrontShell'
import { PainelShell } from '@/features/painel/PainelShell'

/**
 * Storefront publico e painel do dono no mesmo bundle SPA, separados por rota (D5 / spec
 * `design-system`). Shells vazios nesta change; telas reais entram nas changes seguintes.
 */
export const router = createBrowserRouter([
  {
    path: '/',
    element: <StorefrontShell />,
  },
  {
    path: '/painel/*',
    element: <PainelShell />,
  },
])
