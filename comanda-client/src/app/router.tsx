import { Navigate, createBrowserRouter } from 'react-router'

import { LoginPage } from '@/features/auth/LoginPage'
import { RequireAuth } from '@/features/auth/RequireAuth'
import { SignupPage } from '@/features/auth/SignupPage'
import { PrivacyPolicyPage } from '@/features/legal/PrivacyPolicyPage'
import { TermsPage } from '@/features/legal/TermsPage'
import { MenuScreen } from '@/features/menu/MenuScreen'
import { ProductEditorScreen } from '@/features/menu/ProductEditorScreen'
import { OnboardingWizard } from '@/features/onboarding/OnboardingWizard'
import { OrdersScreen } from '@/features/orders/OrdersScreen'
import { AjustesScreen } from '@/features/painel/AjustesScreen'
import { PainelShell } from '@/features/painel/PainelShell'
import { MeuLinkScreen } from '@/features/plans/MeuLinkScreen'
import { PlanoUsoScreen } from '@/features/plans/PlanoUsoScreen'
import { StorefrontShell } from '@/features/storefront/StorefrontShell'

/**
 * Storefront publico e painel do dono no mesmo bundle SPA, separados por rota (D5 / spec
 * `design-system`). `/painel/*` exige sessao (owner-auth); `pedidos` (order-operation) e a tela
 * de operacao do dono, agora o destino padrao do shell (antes apontava para `cardapio` por nao
 * existir ainda).
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
    path: '/onboarding',
    element: <OnboardingWizard />,
  },
  {
    path: '/privacidade',
    element: <PrivacyPolicyPage />,
  },
  {
    path: '/termos',
    element: <TermsPage />,
  },
  {
    path: '/painel',
    element: (
      <RequireAuth>
        <PainelShell />
      </RequireAuth>
    ),
    children: [
      { index: true, element: <Navigate to="pedidos" replace /> },
      { path: 'pedidos', element: <OrdersScreen /> },
      { path: 'cardapio', element: <MenuScreen /> },
      { path: 'cardapio/produtos/novo', element: <ProductEditorScreen /> },
      { path: 'cardapio/produtos/:productId', element: <ProductEditorScreen /> },
      { path: 'ajustes', element: <AjustesScreen /> },
      { path: 'ajustes/meu-link', element: <MeuLinkScreen /> },
      { path: 'ajustes/plano', element: <PlanoUsoScreen /> },
    ],
  },
])
