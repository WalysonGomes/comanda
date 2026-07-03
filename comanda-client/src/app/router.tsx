import { Navigate, createBrowserRouter } from 'react-router'

import { LoginPage } from '@/features/auth/LoginPage'
import { RequireAuth } from '@/features/auth/RequireAuth'
import { SignupPage } from '@/features/auth/SignupPage'
import { MenuScreen } from '@/features/menu/MenuScreen'
import { ProductEditorScreen } from '@/features/menu/ProductEditorScreen'
import { PainelShell } from '@/features/painel/PainelShell'
import { StorefrontShell } from '@/features/storefront/StorefrontShell'

/**
 * Storefront publico e painel do dono no mesmo bundle SPA, separados por rota (D5 / spec
 * `design-system`). `/painel/*` exige sessao (owner-auth); `cardapio/**` (menu-management) e o
 * primeiro conteudo real do shell.
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
    path: '/painel',
    element: (
      <RequireAuth>
        <PainelShell />
      </RequireAuth>
    ),
    children: [
      { index: true, element: <Navigate to="cardapio" replace /> },
      { path: 'cardapio', element: <MenuScreen /> },
      { path: 'cardapio/produtos/novo', element: <ProductEditorScreen /> },
      { path: 'cardapio/produtos/:productId', element: <ProductEditorScreen /> },
    ],
  },
])
