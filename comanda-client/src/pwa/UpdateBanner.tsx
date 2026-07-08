import { RefreshCw } from 'lucide-react'

import { usePwaUpdateStore } from '@/pwa/registerSW'

/**
 * "Nova versão disponível" (task 5.1/5.2) — nunca recarrega sozinho (regra 11 do PRD, D3): o
 * dono decide quando aplicar. `onOfflineReady` (task 5.3) é deliberadamente silencioso — apenas
 * limpa o próprio estado, sem UI, já que "pronto para uso offline" não exige ação do dono.
 */
export function UpdateBanner() {
  const needRefresh = usePwaUpdateStore((state) => state.needRefresh)
  const applyUpdate = usePwaUpdateStore((state) => state.applyUpdate)

  if (!needRefresh) return null

  return (
    <div className="mx-3.5 mt-3 flex items-center gap-[11px] rounded-2xl bg-[#2c2520] p-3 shadow-lg">
      <RefreshCw className="size-5 flex-none text-[#ffb38a]" />
      <div className="min-w-0 flex-1">
        <div className="text-[13px] font-bold leading-tight text-[#f4ece0]">Nova versão disponível</div>
        <div className="mt-0.5 text-[11.5px] leading-snug text-[#b6a890]">Atualize para pegar as últimas mudanças do painel.</div>
      </div>
      <button
        type="button"
        onClick={applyUpdate}
        className="flex-none rounded-[10px] bg-acc px-[13px] py-2.5 text-[12.5px] font-bold text-white"
      >
        Atualizar
      </button>
    </div>
  )
}
