import { Outlet } from 'react-router'

/**
 * Authenticated painel shell (foundations). Real screens render via nested routes, starting
 * with the menu-management screens (`/painel/cardapio/**`).
 */
export function PainelShell() {
  return (
    <div className="min-h-svh bg-cream">
      <Outlet />
    </div>
  )
}
