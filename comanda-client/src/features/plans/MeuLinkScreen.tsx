import { ArrowLeft, Check, Copy, TriangleAlert } from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router'

import { QrCode } from '@/components/QrCode'
import { copyText, shareMenuLink } from '@/lib/share'
import { usePlanStatus } from '@/features/plans/queries'

/** Bloco `MEU LINK` (`.design/Comanda Painel.dc.html` lines 473-491, task 8). */
export function MeuLinkScreen() {
  const navigate = useNavigate()
  const statusQuery = usePlanStatus()
  const status = statusQuery.data
  const [copied, setCopied] = useState(false)

  async function handleCopy() {
    if (!status) return
    const ok = await copyText(`https://${status.menuUrl}`)
    setCopied(ok)
    if (ok) setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className="flex min-h-svh flex-col bg-cream">
      <header className="flex flex-none items-center gap-3 px-4 pt-5 pb-3.5">
        <button
          type="button"
          onClick={() => navigate('/painel/ajustes')}
          aria-label="Voltar"
          className="flex size-9 items-center justify-center rounded-[11px] bg-[#efe6d6] text-ink-2"
        >
          <ArrowLeft className="size-5" strokeWidth={2} />
        </button>
        <h1 className="font-display text-xl font-extrabold text-ink">Meu link</h1>
      </header>

      <div className="flex flex-1 flex-col items-center px-4 pb-8">
        {statusQuery.isLoading || !status ? (
          <div className="sk h-[208px] w-[208px] rounded-[20px]" />
        ) : (
          <>
            <p className="text-xs font-bold tracking-wide text-ink-3 uppercase">Endereço do seu cardápio</p>
            <div className="mt-3.5 rounded-[20px] bg-white p-3.5 shadow-[0_12px_30px_-12px_rgba(60,42,24,.4)]">
              <QrCode value={`https://${status.menuUrl}`} size={208} />
            </div>

            <div className="mt-4.5 flex w-full items-center gap-2.5 rounded-[13px] border-[1.5px] border-line bg-card px-3.5 py-3.5">
              <span className="flex-1 truncate font-mono text-sm font-bold text-acc-d">{status.menuUrl}</span>
              <button type="button" onClick={handleCopy} aria-label="Copiar link" className="flex-none text-ink-2">
                {copied ? <Check className="size-[18px] text-[#2f9e44]" /> : <Copy className="size-[18px]" />}
              </button>
            </div>

            <div className="mt-3 flex w-full gap-2.5">
              <button
                type="button"
                onClick={handleCopy}
                className="flex flex-1 items-center justify-center gap-2 rounded-[13px] border-[1.5px] border-line bg-card py-3.5 text-sm font-bold text-ink"
              >
                <Copy className="size-4" strokeWidth={2} />
                Copiar
              </button>
              <button
                type="button"
                onClick={() => shareMenuLink(status.menuUrl, status.businessName)}
                className="flex flex-[1.3] items-center justify-center gap-2 rounded-[13px] bg-[#1f7a34] py-3.5 text-sm font-bold text-white"
              >
                Compartilhar
              </button>
            </div>

            <div className="mt-4.5 flex gap-2.5 rounded-2xl border border-[rgba(201,136,26,.28)] bg-[#f7ecd2] p-3.5">
              <TriangleAlert className="mt-0.5 size-4 shrink-0 text-[#9a6510]" strokeWidth={2} />
              <p className="text-left text-[12.5px] leading-snug text-[#7a5310]">
                Alterar o subdomínio <b className="font-bold text-[#5e3f0a]">quebra todos os QR Codes impressos</b> e os
                links já compartilhados. Mude apenas se tiver certeza.
              </p>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
