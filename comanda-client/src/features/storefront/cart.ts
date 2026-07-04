import { create } from 'zustand'
import { persist } from 'zustand/middleware'

import { getStorefrontSlug } from '@/features/storefront/subdomain'
import type { DeliveryType } from '@/features/storefront/api'

/**
 * A cart line snapshots the product name/base price and every selected additional's name/price
 * (design.md Decision 4) so the displayed price never "jumps" if the owner edits the menu
 * mid-session — the checkout-time validation (Decision 6) is what reconciles against the server's
 * current prices before the handoff. `selections` (group id -> selected item ids) is kept
 * separately so the product sheet can reopen a line pre-loaded for editing (task 8.4/9.2).
 */
export type CartAdditional = { name: string; price: number }

export type CartLine = {
  lineId: string
  productId: number
  name: string
  basePrice: number
  quantity: number
  note: string
  /** Zero-price single selections, shown as "(Frango grelhado)" next to the product name. */
  specs: string[]
  /** Priced additions, shown as their own line ("- Adicional: Ovo frito (+R$ 2,00)"). */
  adds: CartAdditional[]
  selections: Record<number, number[]>
}

export function unitPrice(line: CartLine): number {
  return line.basePrice + line.adds.reduce((sum, add) => sum + add.price, 0)
}

export function lineTotal(line: CartLine): number {
  return unitPrice(line) * line.quantity
}

type CartState = {
  lines: CartLine[]
  deliveryType: DeliveryType
  addOrUpdateLine: (line: CartLine) => void
  removeLine: (lineId: string) => void
  setQuantity: (lineId: string, quantity: number) => void
  setDeliveryType: (deliveryType: DeliveryType) => void
  clear: () => void
}

export const useCartStore = create<CartState>()(
  persist(
    (set) => ({
      lines: [],
      deliveryType: 'ENTREGA',
      addOrUpdateLine: (line) =>
        set((state) => {
          const exists = state.lines.some((l) => l.lineId === line.lineId)
          return {
            lines: exists
              ? state.lines.map((l) => (l.lineId === line.lineId ? line : l))
              : [...state.lines, line],
          }
        }),
      removeLine: (lineId) => set((state) => ({ lines: state.lines.filter((l) => l.lineId !== lineId) })),
      setQuantity: (lineId, quantity) =>
        set((state) => ({
          lines:
            quantity <= 0
              ? state.lines.filter((l) => l.lineId !== lineId)
              : state.lines.map((l) => (l.lineId === lineId ? { ...l, quantity } : l)),
        })),
      setDeliveryType: (deliveryType) => set({ deliveryType }),
      clear: () => set({ lines: [], deliveryType: 'ENTREGA' }),
    }),
    { name: `comanda-cart-${getStorefrontSlug()}` },
  ),
)
