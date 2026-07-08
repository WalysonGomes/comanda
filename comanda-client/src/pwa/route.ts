/**
 * Discriminador único de contexto painel × storefront (D2/D5): mesmo critério usado para montar
 * `PainelShell` no router (prefixo `/painel`), reaproveitado aqui para que o registro do Service
 * Worker e a injeção de tags do manifest nunca rodem fora do painel.
 */
export function isPainelRoute(pathname: string = window.location.pathname): boolean {
  return pathname === '/painel' || pathname.startsWith('/painel/')
}
