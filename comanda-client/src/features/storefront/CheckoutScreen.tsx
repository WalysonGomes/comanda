import { useState } from 'react'
import { AlertTriangle, ArrowLeft, Bike, ShoppingBag } from 'lucide-react'

import { cn } from '@/lib/utils'
import { formatCurrency } from '@/features/menu/format'
import { storefrontApi, type BusinessInfo, type DeliveryType } from '@/features/storefront/api'
import { lineTotal, useCartStore } from '@/features/storefront/cart'
import { useValidateCart } from '@/features/storefront/queries'
import { buildOrderMessage, buildWhatsAppUrl } from '@/features/storefront/whatsapp'

/** Checkout (block CHECKOUT): single screen, address only for Entrega, inline validation, and a
 * pre-handoff availability re-check against the server (tasks 10.1-10.4). */
export function CheckoutScreen({
  business,
  onBack,
  onSent,
}: {
  business: BusinessInfo
  onBack: () => void
  onSent: (message: string, whatsappUrl: string | null) => void
}) {
  const lines = useCartStore((s) => s.lines)
  const deliveryType = useCartStore((s) => s.deliveryType)
  const setDeliveryType = useCartStore((s) => s.setDeliveryType)
  const removeLine = useCartStore((s) => s.removeLine)

  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [address, setAddress] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [unavailable, setUnavailable] = useState<{ lineId: string; name: string }[]>([])

  const validateCart = useValidateCart()

  const nameOk = name.trim().length >= 2
  const phoneOk = phone.replace(/\D/g, '').length >= 10
  const addressOk = deliveryType === 'RETIRADA' || address.trim().length >= 5

  const deliveryFeeValue = Number(business.deliveryFee)
  const subtotal = lines.reduce((sum, line) => sum + lineTotal(line), 0)
  const fee = deliveryType === 'ENTREGA' ? deliveryFeeValue : 0
  const total = subtotal + fee

  const formValid = nameOk && phoneOk && addressOk && unavailable.length === 0 && business.open && lines.length > 0

  function setDelivery(type: DeliveryType) {
    setDeliveryType(type)
    setUnavailable([])
  }

  function removeUnavailable(lineId: string) {
    removeLine(lineId)
    setUnavailable((u) => u.filter((item) => item.lineId !== lineId))
  }

  async function finalize() {
    setSubmitted(true)
    if (!nameOk || !phoneOk || !addressOk || !business.open || lines.length === 0 || submitting) return

    setSubmitting(true)
    try {
      const payload = {
        lines: lines.map((line) => ({
          lineId: line.lineId,
          productId: line.productId,
          quantity: line.quantity,
          additionalItemIds: Object.values(line.selections).flat(),
        })),
        deliveryType,
      }
      const result = await validateCart.mutateAsync(payload)
      if (!result.valid) {
        const invalidIds = new Set(result.lines.filter((l) => !l.available).map((l) => l.lineId))
        setUnavailable(lines.filter((line) => invalidIds.has(line.lineId)).map((line) => ({ lineId: line.lineId, name: line.name })))
        return
      }
      setUnavailable([])

      const message = buildOrderMessage({ lines, deliveryType, address, deliveryFee: deliveryFeeValue })

      // The idempotent order-creation endpoint is a contract declared here and implemented by
      // `order-operation` (proposal.md Decision 7) — it may not exist yet. The storefront's own
      // requirement is unconditional: confirm "Pedido enviado" before the WhatsApp handoff and
      // always offer the copy fallback, regardless of whether persistence succeeds.
      try {
        await storefrontApi.createOrder({
          idempotencyKey: crypto.randomUUID(),
          customerName: name.trim(),
          customerPhone: phone.trim(),
          deliveryType,
          address: deliveryType === 'ENTREGA' ? address.trim() : null,
          notes: lines.filter((line) => line.note).map((line) => `${line.name}: ${line.note}`).join(' | ') || null,
          lines: payload.lines,
        })
      } catch {
        // Ignored: see comment above.
      }

      const whatsappUrl = business.whatsappNumber ? buildWhatsAppUrl(business.whatsappNumber, message) : null
      onSent(message, whatsappUrl)
    } finally {
      setSubmitting(false)
    }
  }

  const finalizeBlocked = !business.open || unavailable.length > 0
  const finalizeLabel = !business.open ? 'Loja fechada agora' : unavailable.length > 0 ? 'Remova o item indisponível' : submitting ? 'Enviando…' : 'Enviar pedido'

  return (
    <div className="flex min-h-svh flex-col bg-cream">
      <div className="flex flex-none items-center gap-2.5 border-b border-line px-4 pt-2.5 pb-3.5">
        <button
          type="button"
          onClick={onBack}
          aria-label="Voltar ao carrinho"
          className="flex size-9 items-center justify-center rounded-[11px] bg-[#efe6d6] text-ink-2"
        >
          <ArrowLeft className="size-5" />
        </button>
        <h1 className="font-display text-xl font-extrabold text-ink">Finalizar pedido</h1>
      </div>

      <div className="flex-1 overflow-y-auto px-4 pt-4 pb-2">
        {unavailable.map((item) => (
          <div key={item.lineId} className="mb-4 rounded-[14px] border border-[rgba(160,90,76,.3)] bg-[#f0e1dc] p-3.5">
            <div className="flex items-center gap-2 text-[13.5px] font-bold text-[#8a4a3d]">
              <AlertTriangle className="size-[18px]" />
              {item.name} não está mais disponível
            </div>
            <p className="my-1.5 text-[12.5px] leading-snug text-[#9a5a4c]">
              O item ficou indisponível enquanto você montava o pedido. Remova para continuar.
            </p>
            <button
              type="button"
              onClick={() => removeUnavailable(item.lineId)}
              className="w-full rounded-[11px] border border-[#c98a7c] bg-white py-2.5 text-[13.5px] font-bold text-[#8a4a3d]"
            >
              Remover item indisponível
            </button>
          </div>
        ))}

        <p className="mb-2.5 text-xs font-bold text-ink-2">Como você quer receber?</p>
        <div className="flex gap-2.5">
          <button
            type="button"
            onClick={() => setDelivery('ENTREGA')}
            className={cn(
              'flex flex-1 flex-col items-center gap-1.5 rounded-[14px] border-[1.5px] p-3.5',
              deliveryType === 'ENTREGA' ? 'border-acc bg-acc text-white' : 'border-line bg-card text-ink-2',
            )}
          >
            <Bike className="size-[22px]" />
            <span className="text-[13.5px] font-bold">Entrega</span>
          </button>
          <button
            type="button"
            onClick={() => setDelivery('RETIRADA')}
            className={cn(
              'flex flex-1 flex-col items-center gap-1.5 rounded-[14px] border-[1.5px] p-3.5',
              deliveryType === 'RETIRADA' ? 'border-acc bg-acc text-white' : 'border-line bg-card text-ink-2',
            )}
          >
            <ShoppingBag className="size-[22px]" />
            <span className="text-[13.5px] font-bold">Retirada</span>
          </button>
        </div>

        <div className="mt-4.5 flex flex-col gap-3.5">
          <label className="block">
            <span className="mb-1.5 block text-xs font-bold text-ink-2">Seu nome</span>
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Como podemos te chamar?"
              className={cn(
                'w-full rounded-xl border-[1.5px] bg-card p-3.5 text-[15px] text-ink outline-none',
                submitted && !nameOk ? 'border-[#d09b8e]' : 'border-line focus:border-acc',
              )}
            />
            {submitted && !nameOk && <p className="mt-1.5 text-[11.5px] font-semibold text-[#a05a4c]">Informe seu nome.</p>}
          </label>
          <label className="block">
            <span className="mb-1.5 block text-xs font-bold text-ink-2">WhatsApp / telefone</span>
            <input
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="(85) 90000-0000"
              className={cn(
                'w-full rounded-xl border-[1.5px] bg-card p-3.5 text-[15px] text-ink outline-none',
                submitted && !phoneOk ? 'border-[#d09b8e]' : 'border-line focus:border-acc',
              )}
            />
            {submitted && !phoneOk && (
              <p className="mt-1.5 text-[11.5px] font-semibold text-[#a05a4c]">Informe um telefone válido com DDD.</p>
            )}
          </label>
          {deliveryType === 'ENTREGA' && (
            <label className="block">
              <span className="mb-1.5 block text-xs font-bold text-ink-2">Endereço de entrega</span>
              <input
                value={address}
                onChange={(e) => setAddress(e.target.value)}
                placeholder="Rua, número, bairro e complemento"
                className={cn(
                  'w-full rounded-xl border-[1.5px] bg-card p-3.5 text-[15px] text-ink outline-none',
                  submitted && !addressOk ? 'border-[#d09b8e]' : 'border-line focus:border-acc',
                )}
              />
              {submitted && !addressOk && (
                <p className="mt-1.5 text-[11.5px] font-semibold text-[#a05a4c]">Informe o endereço para entrega.</p>
              )}
            </label>
          )}
        </div>

        <div className="mt-4.5 rounded-[14px] border border-line bg-card p-3.5">
          <div className="mb-2 flex justify-between text-[13.5px] text-ink-2">
            <span>Subtotal</span>
            <span className="font-semibold text-ink">{formatCurrency(subtotal)}</span>
          </div>
          <div className="mb-2.5 flex justify-between text-[13.5px] text-ink-2">
            <span>Taxa de entrega</span>
            <span className="font-semibold text-ink">{deliveryType === 'ENTREGA' ? formatCurrency(fee) : 'Grátis'}</span>
          </div>
          <div className="mb-2.5 h-px bg-line" />
          <div className="flex items-baseline justify-between">
            <span className="text-sm font-bold text-ink">Total</span>
            <span className="font-display text-[22px] font-extrabold text-acc-d">{formatCurrency(total)}</span>
          </div>
        </div>
      </div>

      <div className="flex-none border-t border-line bg-cream px-4 pt-3 pb-[calc(12px+env(safe-area-inset-bottom))]">
        <button
          type="button"
          onClick={finalize}
          disabled={submitting || (submitted && !formValid && !finalizeBlocked)}
          className={cn(
            'w-full rounded-2xl py-4 text-[15.5px] font-extrabold text-white shadow-[0_10px_22px_-10px_var(--acc)]',
            finalizeBlocked || submitting ? 'cursor-not-allowed bg-[#d8c3bb] shadow-none' : 'bg-acc',
          )}
        >
          {finalizeLabel}
        </button>
      </div>
    </div>
  )
}
