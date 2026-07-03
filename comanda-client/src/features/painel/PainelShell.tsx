import { LayoutDashboard } from 'lucide-react'

/**
 * Shell vazio do painel do dono (foundations). Telas reais entram em `order-operation` /
 * `menu-management`.
 */
export function PainelShell() {
  return (
    <div className="flex min-h-svh flex-col items-center justify-center gap-3 bg-cream px-6 text-center">
      <LayoutDashboard className="size-9 text-acc" strokeWidth={1.75} />
      <h1 className="font-display text-2xl font-extrabold text-ink">Painel do dono</h1>
      <p className="max-w-xs text-sm text-ink-2">Operacao de pedidos do negocio.</p>
    </div>
  )
}
