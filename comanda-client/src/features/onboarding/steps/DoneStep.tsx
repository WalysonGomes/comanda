import { CircleCheck, Copy, Check } from 'lucide-react'
import { useState } from 'react'

import { QrCode } from '@/components/QrCode'
import { copyText, shareMenuLink } from '@/lib/share'

/** Step 4 (`.design/Comanda Painel.dc.html` `ob4`, lines 205-219): link, QR, copy, share — the
 * cardápio is already published at this point (task 6.6). */
export function DoneStep({ menuUrl, businessName }: { menuUrl: string; businessName: string }) {
  const [copied, setCopied] = useState(false)

  async function handleCopy() {
    const ok = await copyText(`https://${menuUrl}`)
    setCopied(ok)
    if (ok) setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className="flex flex-col items-center px-1 pt-7 pb-2.5 text-center animate-cmd-slide">
      <div className="flex size-[78px] items-center justify-center rounded-full bg-[#e7f4e5] text-[#2f9e44] animate-cmd-check">
        <CircleCheck className="size-10" strokeWidth={1.75} />
      </div>
      <h1 className="mt-5 font-display text-[28px] leading-[1.1] font-black text-ink">
        Seu cardápio
        <br />
        está no ar! 🎉
      </h1>
      <p className="mt-2.5 max-w-[260px] text-[14.5px] leading-snug text-ink-2">
        Compartilhe o link abaixo com seus clientes pelo WhatsApp.
      </p>

      <div className="mt-5.5 w-full rounded-2xl border-[1.5px] border-line bg-card p-4">
        <div className="flex items-center gap-2.5 rounded-[11px] bg-[#efe6d6] px-3.5 py-2.5">
          <span className="flex-1 truncate text-left font-mono text-[13.5px] font-bold text-acc-d">{menuUrl}</span>
          <button type="button" onClick={handleCopy} aria-label="Copiar link" className="flex-none text-ink-2">
            {copied ? <Check className="size-[18px] text-[#2f9e44]" /> : <Copy className="size-[18px]" />}
          </button>
        </div>

        <div className="mt-4 flex justify-center">
          <div className="rounded-[14px] bg-white p-2.5 shadow-[0_4px_14px_-6px_rgba(60,42,24,.3)]">
            <QrCode value={`https://${menuUrl}`} />
          </div>
        </div>

        <button
          type="button"
          onClick={() => shareMenuLink(menuUrl, businessName)}
          className="mt-4 flex w-full items-center justify-center gap-2 rounded-xl border border-[#2f9e44] bg-[#e7f4e5] py-3 text-sm font-bold text-[#1f7a34]"
        >
          Compartilhar no WhatsApp
        </button>
      </div>
    </div>
  )
}
