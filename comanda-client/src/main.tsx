import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { injectPainelHeadTags } from '@/pwa/headTags'
import { registerPainelServiceWorker } from '@/pwa/registerSW'
import { isPainelRoute } from '@/pwa/route'

// owner-pwa (D2): manifest + Service Worker existem apenas no contexto do painel — o storefront
// nunca referencia o manifest nem registra o SW (spec "Storefront do cliente permanece sem PWA").
if (isPainelRoute()) {
  injectPainelHeadTags()
  registerPainelServiceWorker()
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
