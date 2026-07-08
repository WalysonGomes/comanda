import { registerSW } from 'virtual:pwa-register'
import { create } from 'zustand'

type PwaUpdateState = {
  needRefresh: boolean
  offlineReady: boolean
  /** D3: só recarrega no clique do dono — nunca reload silencioso ao detectar nova versão. */
  applyUpdate: () => void
  dismissOfflineReady: () => void
}

let updateServiceWorker: ((reloadPage?: boolean) => Promise<void>) | null = null

export const usePwaUpdateStore = create<PwaUpdateState>((set) => ({
  needRefresh: false,
  offlineReady: false,
  applyUpdate: () => {
    void updateServiceWorker?.(true)
  },
  dismissOfflineReady: () => set({ offlineReady: false }),
}))

/**
 * Registro isolado do Service Worker do painel (D2). Chamado uma única vez no bootstrap
 * (`main.tsx`), apenas quando `isPainelRoute` é verdadeiro — nunca no storefront. Desabilitar
 * (ex.: futuro empacotamento nativo Capacitor) é apenas não chamar esta função; nenhuma tela do
 * painel depende dela diretamente (task 3.4).
 */
export function registerPainelServiceWorker(): void {
  updateServiceWorker = registerSW({
    onNeedRefresh() {
      usePwaUpdateStore.setState({ needRefresh: true })
    },
    onOfflineReady() {
      usePwaUpdateStore.setState({ offlineReady: true })
    },
  })
}
