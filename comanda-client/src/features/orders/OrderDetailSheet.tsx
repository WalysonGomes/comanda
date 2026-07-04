import { useState } from 'react'
import { Bike, Check, ShoppingBag, User, X } from 'lucide-react'

import { CancelSheet } from '@/features/orders/CancelSheet'
import { NEXT_LABEL, NEXT_STATUS, STATUS_META, STEPS, formatCurrency, formatRelativeTime, waMessageForAdvance } from '@/features/orders/format'
import { useAdvanceOrder, useOrderDetail } from '@/features/orders/queries'

export function OrderDetailSheet({ orderId, onClose }: { orderId: number; onClose: () => void }) {
  const detailQuery = useOrderDetail(orderId)
  const advance = useAdvanceOrder(orderId)
  const [cancelOpen, setCancelOpen] = useState(false)

  const order = detailQuery.data
  if (!order) {
    return null
  }

  const meta = STATUS_META[order.status]
  const isCancelled = order.status === 'CANCELADO'
  const isDone = order.status === 'ENTREGUE'
  const nextStatus = NEXT_STATUS[order.status]
  const nextLabel = NEXT_LABEL[order.status]
  const canCancel = !isCancelled && !isDone
  const isDelivery = order.deliveryType === 'ENTREGA'
  const currentStep = meta.step

  const waPreview = nextStatus ? waMessageForAdvance(order.customerName, order.shortCode, nextStatus, order.deliveryType) : ''

  function sendWhatsApp() {
    let digits = order!.customerPhone.replace(/\D/g, '')
    if (!digits.startsWith('55')) digits = '55' + digits
    window.open(`https://wa.me/${digits}?text=${encodeURIComponent(waPreview)}`, '_blank')
  }

  return (
    <div className="absolute inset-0 z-20">
      <div onClick={onClose} className="absolute inset-0 animate-cmd-fade bg-[rgba(28,24,19,.42)]" />
      <div className="absolute inset-x-0 bottom-0 flex max-h-[90%] animate-cmd-up flex-col rounded-t-[26px] bg-cream shadow-2xl">
        <div className="flex flex-none justify-center pb-1 pt-2.5">
          <div className="h-[5px] w-[42px] rounded-full bg-[#dac9b0]" />
        </div>
        <div className="flex flex-none items-center justify-between border-b border-line px-[18px] pb-3 pt-1.5">
          <div className="flex items-center gap-2.5">
            <span className="font-mono text-[15px] font-bold text-ink">{order.shortCode}</span>
            <span
              className="inline-flex items-center gap-[5px] rounded-lg px-[9px] py-[5px] text-[11.5px] font-bold"
              style={{ background: meta.bg, color: meta.fg }}
            >
              <span className="size-1.5 rounded-full" style={{ background: meta.accent }} />
              {meta.label}
            </span>
          </div>
          <button type="button" onClick={onClose} aria-label="Fechar" className="flex size-[34px] items-center justify-center rounded-[10px] bg-[#efe6d6] text-ink-2">
            <X className="size-[18px]" />
          </button>
        </div>

        <div className="shb flex-1 overflow-y-auto px-[18px] pb-2 pt-4">
          {isCancelled && (
            <div className="mb-4 rounded-2xl border border-[rgba(160,90,76,.25)] bg-[#f0e1dc] p-[13px]">
              <div className="flex items-center gap-2 text-[13px] font-bold text-[#8a4a3d]">
                <X className="size-[17px]" />
                Pedido cancelado
              </div>
              <div className="mt-[7px] text-[13px] leading-snug text-[#9a5a4c]">{order.cancellationReason}</div>
            </div>
          )}

          {!isCancelled && (
            <div className="flex px-0.5 pb-[18px] pt-1">
              {STEPS.map((step, i) => {
                const reached = currentStep >= 0 && i <= currentStep
                return (
                  <div key={step.label} className="flex flex-1 flex-col items-center">
                    <div className="flex w-full items-center">
                      <div className="h-[3px] flex-1 rounded" style={{ background: i === 0 ? 'transparent' : reached ? '#dcc59c' : '#ece3d3' }} />
                      <div
                        className="size-4 flex-none rounded-full"
                        style={{ background: reached ? step.color : '#e6dccb', boxShadow: i === currentStep ? `0 0 0 4px ${step.color}33` : 'none' }}
                      />
                      <div className="h-[3px] flex-1 rounded" style={{ background: i === 4 ? 'transparent' : reached && i < currentStep ? '#dcc59c' : '#ece3d3' }} />
                    </div>
                    <span className="mt-[7px] text-[10.5px] font-bold leading-tight" style={{ color: reached ? 'var(--ink)' : '#b3a691' }}>
                      {step.label}
                    </span>
                  </div>
                )
              })}
            </div>
          )}

          <div className="flex flex-col gap-[11px] rounded-2xl border border-line bg-card p-3.5">
            <div className="flex items-center gap-2.5">
              <User className="size-[17px] text-ink-3" />
              <div className="flex-1">
                <div className="text-[14.5px] font-bold leading-tight text-ink">{order.customerName}</div>
                <div className="mt-[3px] font-mono text-[12.5px] font-semibold text-ink-2">{order.customerPhone}</div>
              </div>
            </div>
            <div className="h-px bg-line" />
            <div className="flex items-start gap-2.5">
              {isDelivery ? <Bike className="mt-0.5 size-[17px] text-ink-3" /> : <ShoppingBag className="mt-0.5 size-[17px] text-ink-3" />}
              <div className="flex-1">
                <div className="text-[13.5px] font-bold leading-tight text-ink">{isDelivery ? 'Entrega' : 'Retirada'}</div>
                <div className="mt-0.5 text-[12.5px] leading-snug text-ink-2">{isDelivery ? order.address : 'Retirada no balcão'}</div>
              </div>
              <span className="text-xs font-semibold text-ink-3">{formatRelativeTime(order.createdAt)}</span>
            </div>
          </div>

          <div className="mt-4">
            <div className="mb-2.5 text-[11.5px] font-bold uppercase tracking-widest text-ink-3">Itens do pedido</div>
            <div className="flex flex-col gap-2.5">
              {order.items.map((item, i) => (
                <div key={i} className="rounded-2xl border border-line bg-card p-3.5">
                  <div className="flex items-start justify-between gap-2.5">
                    <div className="flex flex-1 items-start gap-2.5">
                      <span className="flex h-6 min-w-[26px] flex-none items-center justify-center rounded-md bg-acc-tint px-1.5 font-bold text-acc-d">
                        {item.quantity}
                      </span>
                      <span className="text-[14.5px] font-bold leading-snug text-ink">{item.productName}</span>
                    </div>
                    <span className="whitespace-nowrap font-display text-sm font-extrabold text-ink">{formatCurrency(item.subtotal)}</span>
                  </div>
                  {item.additionals.length > 0 && (
                    <div className="ml-[35px] mt-2.5 flex flex-col gap-1">
                      {item.additionals.map((a, ai) => (
                        <div key={ai} className="flex items-center justify-between">
                          <span className="flex items-center gap-1.5 text-[12.5px] text-ink-2">
                            <span className="size-1 rounded-full bg-[#c9b89a]" />
                            {a.name}
                          </span>
                          <span className="text-xs font-semibold text-ink-3">+ {formatCurrency(a.price)}</span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>

          {order.notes && (
            <div className="mt-[13px] rounded-xl bg-[#f7ecd2] p-3">
              <span className="font-bold text-[#5e3f0a]">Obs:</span>{' '}
              <span className="text-[12.5px] leading-snug text-[#7a5310]">{order.notes}</span>
            </div>
          )}

          <div className="mt-4 rounded-2xl border border-line bg-card p-3.5">
            <div className="mb-2 flex justify-between text-[13.5px] text-ink-2">
              <span>Subtotal</span>
              <span className="font-semibold text-ink">{formatCurrency(order.subtotal)}</span>
            </div>
            <div className="mb-2.5 flex justify-between text-[13.5px] text-ink-2">
              <span>Taxa de entrega</span>
              <span className="font-semibold text-ink">{formatCurrency(order.deliveryFee)}</span>
            </div>
            <div className="mb-2.5 h-px bg-line" />
            <div className="flex items-baseline justify-between">
              <span className="text-sm font-bold text-ink">Total</span>
              <span className="font-display text-[22px] font-extrabold text-acc-d">{formatCurrency(order.total)}</span>
            </div>
          </div>

          {nextStatus && (
            <div className="mt-4">
              <div className="mb-2 flex items-center justify-between">
                <span className="text-[11.5px] font-bold uppercase tracking-widest text-ink-3">Mensagem para o cliente</span>
                <span className="text-[11px] text-ink-3">opcional</span>
              </div>
              <div className="rounded-2xl border border-[#bfe0b8] bg-[#e7f4e5] p-[13px]">
                <div className="whitespace-pre-wrap text-[13px] leading-relaxed text-[#234d22]">{waPreview}</div>
                <button
                  type="button"
                  onClick={sendWhatsApp}
                  className="mt-[11px] flex w-full items-center justify-center gap-2 rounded-[11px] border border-[#2f9e44] bg-white py-2.5 text-[13.5px] font-bold text-[#1f7a34]"
                >
                  Enviar no WhatsApp
                </button>
              </div>
            </div>
          )}
          <div className="h-2" />
        </div>

        <div className="flex-none border-t border-line px-[18px] pb-[calc(12px+env(safe-area-inset-bottom))] pt-3">
          {nextStatus && nextLabel && (
            <button
              type="button"
              disabled={advance.isPending}
              onClick={() => advance.mutate(order.status)}
              className="flex w-full items-center justify-center gap-[9px] rounded-2xl bg-acc py-[15px] font-bold text-white shadow-lg disabled:opacity-70"
            >
              {advance.isPending && <span className="size-[17px] animate-cmd-spin rounded-full border-[2.4px] border-white/40 border-t-white" />}
              {nextLabel}
            </button>
          )}
          {isDone && (
            <div className="flex w-full items-center justify-center gap-2 rounded-2xl bg-[#eee8dd] py-3.5 font-bold text-[#6a6052]">
              <Check className="size-[18px]" strokeWidth={2.4} />
              Pedido concluído
            </div>
          )}
          {advance.isError && (
            <div className="mt-2 flex items-center gap-1.5 text-[12.5px] font-semibold text-[#a05a4c]">
              Não foi possível avançar o pedido. Tente novamente.
            </div>
          )}
          {canCancel && (
            <button type="button" onClick={() => setCancelOpen(true)} className="mt-[9px] w-full py-2 text-[13.5px] font-semibold text-[#a05a4c]">
              Cancelar pedido
            </button>
          )}
        </div>
      </div>

      {cancelOpen && (
        <CancelSheet
          orderId={orderId}
          shortCode={order.shortCode}
          onClose={() => setCancelOpen(false)}
          onCancelled={() => {
            setCancelOpen(false)
          }}
        />
      )}
    </div>
  )
}
