import { Outlet } from 'react-router'

import { PwaBanners } from '@/pwa/PwaBanners'

/**
 * Authenticated painel shell (foundations). Real screens render via nested routes, starting
 * with the menu-management screens (`/painel/cardapio/**`). `PwaBanners` (spec `owner-pwa`)
 * lives here so instalação/atualização aparecem em qualquer tela do painel, nunca no storefront.
 */
export function PainelShell() {
  return (
    <div className="min-h-svh bg-cream">
      <PwaBanners />
      <Outlet />
    </div>
  )
}
