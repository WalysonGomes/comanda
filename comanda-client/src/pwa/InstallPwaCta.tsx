import { useState } from 'react'
import { Download, Share, X } from 'lucide-react'

import { isIos, useInstallPrompt } from '@/pwa/useInstallPrompt'

/**
 * CTA de instalação (task 4.2/4.3/4.4): dispara o prompt nativo no Android; em iOS (sem
 * `beforeinstallprompt`) mostra instrução manual de "Adicionar à Tela de Início". Some quando o
 * app já roda em modo `standalone` (instalado).
 */
export function InstallPwaCta() {
  const { canInstall, installed, promptInstall } = useInstallPrompt()
  const [iosDismissed, setIosDismissed] = useState(false)

  if (installed) return null

  if (canInstall) {
    return (
      <div className="mx-3.5 mt-3 flex items-center gap-[11px] rounded-2xl bg-[#2c2520] p-3 shadow-lg">
        <Download className="size-5 flex-none text-[#ffb38a]" />
        <div className="min-w-0 flex-1">
          <div className="text-[13px] font-bold leading-tight text-[#f4ece0]">Instale o Comanda</div>
          <div className="mt-0.5 text-[11.5px] leading-snug text-[#b6a890]">
            Acesso rápido pela tela inicial, com telas já abertas funcionando offline.
          </div>
        </div>
        <button
          type="button"
          onClick={promptInstall}
          className="flex-none rounded-[10px] bg-acc px-[13px] py-2.5 text-[12.5px] font-bold text-white"
        >
          Instalar
        </button>
      </div>
    )
  }

  if (isIos() && !iosDismissed) {
    return (
      <div className="mx-3.5 mt-3 flex items-center gap-[11px] rounded-2xl bg-[#2c2520] p-3 shadow-lg">
        <Share className="size-5 flex-none text-[#ffb38a]" />
        <div className="min-w-0 flex-1">
          <div className="text-[13px] font-bold leading-tight text-[#f4ece0]">Adicione à Tela de Início</div>
          <div className="mt-0.5 text-[11.5px] leading-snug text-[#b6a890]">
            Toque em Compartilhar e depois em "Adicionar à Tela de Início".
          </div>
        </div>
        <button
          type="button"
          onClick={() => setIosDismissed(true)}
          aria-label="Dispensar"
          className="flex-none text-[#b6a890]"
        >
          <X className="size-4" />
        </button>
      </div>
    )
  }

  return null
}
