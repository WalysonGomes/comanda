import { useEffect, useState } from 'react'
import { Check, Copy, FileText, Info } from 'lucide-react'

/**
 * Block SENT/HANDOFF (tasks 11.1-11.5): confirmation always precedes the WhatsApp handoff, and
 * the copy fallback is a first-class, always-visible part of the screen (PRD 4.5) — not an error
 * state that only appears if `wa.me` fails, since a popup blocker can silently swallow the
 * automatic attempt below.
 */
export function SentScreen({
  message,
  whatsappUrl,
  onNewOrder,
}: {
  message: string
  whatsappUrl: string | null
  onNewOrder: () => void
}) {
  const [copied, setCopied] = useState(false)

  useEffect(() => {
    if (whatsappUrl) {
      window.open(whatsappUrl, '_blank', 'noopener,noreferrer')
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function copyMessage() {
    navigator.clipboard.writeText(message).then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    })
  }

  return (
    <div className="flex min-h-svh flex-col bg-cream px-4">
      <div className="flex flex-col items-center px-2 pt-9 pb-1.5 text-center">
        <div className="flex size-20 items-center justify-center rounded-full bg-[#e7f4e5] text-[#2f9e44] animate-cmd-check">
          <Check className="size-10" strokeWidth={2.4} />
        </div>
        <h1 className="mt-4.5 font-display text-[27px] font-black text-ink">Tudo pronto!</h1>
        <p className="mt-2.5 max-w-[270px] text-sm leading-normal text-ink-2">
          Seu pedido foi montado. Agora é só enviar pelo WhatsApp para o restaurante confirmar.
        </p>
      </div>

      <div className="mt-5 rounded-2xl border border-line bg-card p-4">
        <div className="mb-2.5 flex items-center gap-2 text-[11.5px] font-bold tracking-wide text-ink-3 uppercase">
          <FileText className="size-4" />
          Mensagem do pedido
        </div>
        <p className="text-[13px] leading-relaxed whitespace-pre-wrap text-ink">{message}</p>
      </div>

      {whatsappUrl && (
        <a
          href={whatsappUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="mt-4 flex w-full items-center justify-center gap-2 rounded-2xl bg-[#1f7a34] py-4 text-[15.5px] font-extrabold text-white shadow-[0_12px_24px_-10px_#1f7a34]"
        >
          Abrir no WhatsApp
        </a>
      )}

      <div className="mt-2.5 flex gap-2 rounded-xl border border-[rgba(201,136,26,.24)] bg-[#f7ecd2] px-3.5 py-2.5">
        <Info className="mt-0.5 size-[15px] flex-none text-[#9a6510]" />
        <p className="text-xs leading-snug text-[#7a5310]">
          O WhatsApp não abriu? Copie a mensagem abaixo e cole na conversa do restaurante.
        </p>
      </div>

      <button
        type="button"
        onClick={copyMessage}
        className="mt-2.5 flex w-full items-center justify-center gap-2 rounded-[13px] border-[1.5px] border-line bg-card py-3.5 text-sm font-bold text-ink"
      >
        {copied ? <Check className="size-[17px]" /> : <Copy className="size-[17px]" />}
        {copied ? 'Mensagem copiada' : 'Copiar mensagem'}
      </button>

      <button
        type="button"
        onClick={onNewOrder}
        className="my-4 text-center text-[13.5px] font-bold text-ink-3"
      >
        Fazer novo pedido
      </button>
    </div>
  )
}
