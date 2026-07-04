import { AlertCircle, Minus, Plus, Trash2, X } from 'lucide-react'

import { cn } from '@/lib/utils'
import { formatCurrency } from '@/features/menu/format'
import { lineTotal, useCartStore, type CartLine } from '@/features/storefront/cart'

/** Cart bottom sheet (block CART SHEET): per-line quantity/additionals editing without recreating
 * the line (task 9.1/9.2), discriminated totals, and the minimum-order block (task 9.3/9.4). */
export function CartSheet({
  businessOpen,
  minOrderValue,
  deliveryFee,
  onClose,
  onEditLine,
  onCheckout,
}: {
  businessOpen: boolean
  minOrderValue: number
  deliveryFee: number
  onClose: () => void
  onEditLine: (line: CartLine) => void
  onCheckout: () => void
}) {
  const lines = useCartStore((s) => s.lines)
  const deliveryType = useCartStore((s) => s.deliveryType)
  const setQuantity = useCartStore((s) => s.setQuantity)
  const removeLine = useCartStore((s) => s.removeLine)

  const subtotal = lines.reduce((sum, line) => sum + lineTotal(line), 0)
  const fee = deliveryType === 'ENTREGA' ? deliveryFee : 0
  const total = subtotal + fee
  const belowMin = subtotal > 0 && subtotal < minOrderValue
  const missing = Math.max(0, minOrderValue - subtotal)
  const canContinue = businessOpen && !belowMin && lines.length > 0

  return (
    <div className="fixed inset-0 z-25">
      <div className="absolute inset-0 animate-cmd-fade bg-[#1c1813]/42" onClick={onClose} />
      <div className="absolute inset-x-0 bottom-0 flex max-h-[92%] flex-col rounded-t-[26px] bg-cream shadow-[0_-20px_50px_-20px_rgba(28,24,19,.5)] animate-cmd-up">
        <div className="flex flex-none justify-center pt-2.5 pb-1">
          <div className="h-[5px] w-[42px] rounded-full bg-[#dac9b0]" />
        </div>
        <div className="flex flex-none items-center justify-between border-b border-line px-[18px] pt-1.5 pb-3">
          <span className="font-display text-[19px] font-extrabold text-ink">Seu carrinho</span>
          <button
            type="button"
            onClick={onClose}
            aria-label="Fechar"
            className="flex size-[34px] items-center justify-center rounded-[10px] bg-[#efe6d6] text-ink-2"
          >
            <X className="size-[18px]" strokeWidth={2.2} />
          </button>
        </div>

        <div className="flex flex-1 flex-col gap-3 overflow-y-auto px-[18px] py-3.5">
          {lines.length === 0 && <p className="py-8 text-center text-sm text-ink-3">Seu carrinho está vazio.</p>}
          {lines.map((line) => {
            const detail = [...line.specs, ...line.adds.map((a) => `+ ${a.name}`)].join(' · ')
            return (
              <div key={line.lineId} className="rounded-[15px] border border-line bg-card p-3.5">
                <div className="flex items-start justify-between gap-2.5">
                  <div className="min-w-0 flex-1">
                    <div className="text-[15px] font-bold text-ink">{line.name}</div>
                    {detail && <div className="mt-0.5 text-[12.5px] text-ink-3">{detail}</div>}
                    {line.note && <div className="mt-1 text-xs text-[#9a6510]">Obs: {line.note}</div>}
                  </div>
                  <span className="font-display text-[15px] font-extrabold whitespace-nowrap text-ink">
                    {formatCurrency(lineTotal(line))}
                  </span>
                </div>
                <div className="mt-2.5 flex items-center justify-between">
                  <div className="flex items-center gap-3.5 rounded-[10px] bg-[#f4ecdd] px-1.5 py-1">
                    <button
                      type="button"
                      onClick={() => setQuantity(line.lineId, line.quantity - 1)}
                      aria-label="Diminuir quantidade"
                      className="flex size-7 items-center justify-center rounded-lg bg-card text-ink"
                    >
                      <Minus className="size-4" strokeWidth={2.4} />
                    </button>
                    <span className="min-w-4 text-center font-display text-[15px] font-extrabold text-ink">
                      {line.quantity}
                    </span>
                    <button
                      type="button"
                      onClick={() => setQuantity(line.lineId, line.quantity + 1)}
                      aria-label="Aumentar quantidade"
                      className="flex size-7 items-center justify-center rounded-lg bg-card text-ink"
                    >
                      <Plus className="size-4" strokeWidth={2.4} />
                    </button>
                  </div>
                  <div className="flex items-center gap-3.5">
                    <button
                      type="button"
                      onClick={() => onEditLine(line)}
                      className="text-[12.5px] font-bold text-acc-d"
                    >
                      Editar
                    </button>
                    <button type="button" onClick={() => removeLine(line.lineId)} aria-label="Remover item" className="text-ink-3">
                      <Trash2 className="size-[17px]" />
                    </button>
                  </div>
                </div>
              </div>
            )
          })}
        </div>

        <div className="flex-none border-t border-line bg-cream px-[18px] pt-3.5 pb-[calc(14px+env(safe-area-inset-bottom))]">
          <div className="mb-1.5 flex justify-between text-[13.5px] text-ink-2">
            <span>Subtotal</span>
            <span className="font-semibold text-ink">{formatCurrency(subtotal)}</span>
          </div>
          <div className="mb-2.5 flex justify-between text-[13.5px] text-ink-2">
            <span>Taxa de entrega</span>
            <span className="font-semibold text-ink">{deliveryType === 'ENTREGA' ? formatCurrency(fee) : 'Grátis'}</span>
          </div>
          <div className="mb-3 h-px bg-line" />
          <div className="mb-3 flex items-baseline justify-between">
            <span className="text-sm font-bold text-ink">Total</span>
            <span className="font-display text-[21px] font-extrabold text-acc-d">{formatCurrency(total)}</span>
          </div>
          {belowMin && (
            <div className="mb-2.5 flex items-center gap-2 rounded-xl border border-[rgba(214,73,47,.22)] bg-acc-tint px-3 py-2.5">
              <AlertCircle className="size-4 flex-none text-acc" />
              <span className="text-[12.5px] leading-snug font-semibold text-[#8a4a3a]">
                Faltam <b className="font-extrabold text-[#7a2417]">{formatCurrency(missing)}</b> para atingir o pedido mínimo.
              </span>
            </div>
          )}
          <button
            type="button"
            disabled={!canContinue}
            onClick={onCheckout}
            className={cn(
              'w-full rounded-2xl py-4 text-[15.5px] font-extrabold text-white shadow-[0_10px_22px_-10px_var(--acc)]',
              canContinue ? 'bg-acc' : 'cursor-not-allowed bg-[#d8c3bb] shadow-none',
            )}
          >
            {!businessOpen ? 'Loja fechada agora' : belowMin ? `Faltam ${formatCurrency(missing)} para o mínimo` : 'Continuar'}
          </button>
        </div>
      </div>
    </div>
  )
}
