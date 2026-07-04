import { useState } from 'react'
import { AlertTriangle } from 'lucide-react'

import { useCancelOrder } from '@/features/orders/queries'

const MIN_LENGTH = 10

export function CancelSheet({
  orderId,
  shortCode,
  onClose,
  onCancelled,
}: {
  orderId: number
  shortCode: string
  onClose: () => void
  onCancelled: () => void
}) {
  const [reason, setReason] = useState('')
  const cancel = useCancelOrder(orderId)

  const length = reason.trim().length
  const valid = length >= MIN_LENGTH

  function confirm() {
    if (!valid) return
    cancel.mutate(reason.trim(), { onSuccess: onCancelled })
  }

  return (
    <div className="absolute inset-0 z-30">
      <div onClick={onClose} className="absolute inset-0 animate-cmd-fade bg-[rgba(28,24,19,.5)]" />
      <div className="absolute inset-x-0 bottom-0 animate-cmd-up rounded-t-[26px] bg-cream p-[22px] pb-[calc(20px+env(safe-area-inset-bottom))] shadow-2xl">
        <div className="flex items-center gap-2.5">
          <div className="flex size-[38px] flex-none items-center justify-center rounded-xl bg-[#f0e1dc] text-[#a05a4c]">
            <AlertTriangle className="size-5" />
          </div>
          <div>
            <div className="font-display text-base font-extrabold text-ink">Cancelar pedido {shortCode}</div>
            <div className="mt-0.5 text-[12.5px] text-ink-2">Essa ação não pode ser desfeita.</div>
          </div>
        </div>

        <div className="mb-2 mt-[18px] text-[12.5px] font-bold text-ink-2">Motivo do cancelamento</div>
        <textarea
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          placeholder="Ex.: Cliente desistiu do pedido pelo WhatsApp."
          className="h-[92px] w-full resize-none rounded-[13px] border-[1.5px] bg-card p-3 text-sm leading-relaxed text-ink"
          style={{ borderColor: valid ? '#1f8a52' : length > 0 ? '#d6a23a' : 'var(--line)' }}
        />
        <div
          className="mt-[7px] flex justify-end text-[11.5px] font-semibold"
          style={{ color: valid ? '#1f7a34' : 'var(--ink-3)' }}
        >
          {valid ? 'Pronto para confirmar' : `${length}/${MIN_LENGTH} caracteres mínimos`}
        </div>

        {cancel.isError && (
          <div className="mt-2 text-[12.5px] font-semibold text-[#a05a4c]">Não foi possível cancelar. Tente novamente.</div>
        )}

        <div className="mt-3.5 flex gap-2.5">
          <button
            type="button"
            onClick={onClose}
            className="flex-1 rounded-[13px] border-[1.5px] border-line bg-card py-3.5 text-sm font-bold text-ink-2"
          >
            Voltar
          </button>
          <button
            type="button"
            disabled={!valid || cancel.isPending}
            onClick={confirm}
            className="flex-[1.5] rounded-[13px] py-3.5 text-sm font-extrabold text-white disabled:cursor-not-allowed"
            style={{ background: valid ? '#a04030' : '#d8c3bb' }}
          >
            Confirmar
          </button>
        </div>
      </div>
    </div>
  )
}
