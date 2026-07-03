import { UtensilsCrossed } from 'lucide-react'

/**
 * Shell vazio do storefront publico (foundations). Telas reais entram em `public-storefront`.
 */
export function StorefrontShell() {
  return (
    <div className="flex min-h-svh flex-col items-center justify-center gap-3 bg-cream px-6 text-center">
      <UtensilsCrossed className="size-9 text-acc" strokeWidth={1.75} />
      <h1 className="font-display text-2xl font-extrabold text-ink">Comanda</h1>
      <p className="max-w-xs text-sm text-ink-2">Vitrine do cardapio publico.</p>
    </div>
  )
}
