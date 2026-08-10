import { Navigate, createBrowserRouter } from 'react-router'

import { LoginPage } from '@/features/auth/LoginPage'
import { RequireAuth } from '@/features/auth/RequireAuth'
import { SignupPage } from '@/features/auth/SignupPage'
import { LandingPage } from '@/features/landing/LandingPage'
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
import { hasTenantSubdomain } from '@/features/storefront/subdomain'

function RootRoute() {
  const isTenant = hasTenantSubdomain()
  if (isTenant) {
    return <StorefrontShell />
  }
  return <LandingPage />
}

export const router = createBrowserRouter([
  {
    path: '/',
    element: <RootRoute />,
  },
  {
    path: '/landing',
    element: <LandingPage />,
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
