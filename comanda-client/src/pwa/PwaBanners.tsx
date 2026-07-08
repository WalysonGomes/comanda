import { InstallPwaCta } from '@/pwa/InstallPwaCta'
import { UpdateBanner } from '@/pwa/UpdateBanner'

/**
 * Ponto único de montagem dos avisos de PWA do painel (atualização de versão + instalação).
 * Overlay fixo (não entra no fluxo do document) para não interferir no layout `min-h-svh` que
 * cada tela do painel já define para si mesma.
 */
export function PwaBanners() {
  return (
    <div className="pointer-events-none fixed inset-x-0 top-0 z-50 flex flex-col gap-2">
      <div className="pointer-events-auto">
        <UpdateBanner />
      </div>
      <div className="pointer-events-auto">
        <InstallPwaCta />
      </div>
    </div>
  )
}
