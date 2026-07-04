import { Clock, X } from 'lucide-react'

/**
 * Floating "closed" notice (task 12.1/12.2): browsing and building the cart stay allowed while
 * closed — only checkout is blocked, and always with a visible reason (Regra 11), never silently.
 */
export function ClosedNotice({
  reopensLabel,
  bottom,
  onDismiss,
}: {
  reopensLabel: string | null
  bottom: number
  onDismiss: () => void
}) {
  return (
    <div
      className="absolute left-3.5 right-3.5 z-[14] flex items-start gap-2.5 rounded-[15px] border border-[rgba(201,136,26,.32)] bg-[#fbf3dd] p-3 shadow-[0_16px_30px_-14px_rgba(90,64,20,.35)] animate-cmd-pop"
      style={{ bottom }}
    >
      <Clock className="mt-0.5 size-[18px] flex-none text-[#9a6510]" />
      <div className="min-w-0 flex-1">
        <div className="text-[13px] font-bold text-[#7a5310]">Fechado agora</div>
        <p className="mt-0.5 text-xs leading-snug text-[#8a6521]">
          {reopensLabel ?? 'Você pode montar o carrinho e finalizar no horário de atendimento.'}
        </p>
      </div>
      <button
        type="button"
        onClick={onDismiss}
        aria-label="Dispensar aviso"
        className="flex size-7 flex-none items-center justify-center rounded-[9px] bg-[rgba(201,136,26,.12)] text-[#9a6510]"
      >
        <X className="size-4" />
      </button>
    </div>
  )
}
